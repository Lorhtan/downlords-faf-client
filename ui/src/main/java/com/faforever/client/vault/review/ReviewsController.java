package com.faforever.client.vault.review;


import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.review.Review;
import com.faforever.client.review.ReviewsSummary;
import com.faforever.client.theme.UiService;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReviewsController implements Controller<Pane> {
  private static final int REVIEWS_PER_PAGE = 4;
  private final I18n i18n;
  private final UiService uiService;
  private final PlayerService playerService;

  public Pane reviewsRoot;
  public Label scoreLabel;
  public Pane fiveStarsBar;
  public Pane fourStarsBar;
  public Pane threeStarsBar;
  public Pane twoStarsBar;
  public Pane oneStarBar;
  public Label totalReviewsLabel;
  public Button createReviewButton;
  public StarsController averageStarsController;
  /**
   * Not named `ownReviewController` because `ownReview` clashes with {@link #ownReview}.
   */
  public ReviewController ownReviewPaneController;
  public Label ownReviewLabel;
  public Pane otherReviewsContainer;
  public Pane reviewsPagination;
  public Button pageLeftButton;
  public Button pageRightButton;

  private Consumer<Review> onSendReviewListener;
  private Pane ownReviewRoot;
  private Consumer<Review> onDeleteReviewListener;
  private Optional<Review> ownReview;
  private int currentReviewPage;
  private ReviewsSupplier supplier;
  private IntegerProperty numberOfReviews;

  public ReviewsController(I18n i18n, UiService uiService, PlayerService playerService) {
    this.i18n = i18n;
    this.uiService = uiService;
    this.playerService = playerService;
    ownReview = Optional.empty();
    numberOfReviews = new SimpleIntegerProperty();
  }

  public void initialize() {
    ownReviewRoot = ownReviewPaneController.getRoot();
    JavaFxUtil.setAnchors(ownReviewRoot, 0d);
    ownReviewRoot.managedProperty().bind(ownReviewRoot.visibleProperty());
    ownReviewRoot.setVisible(false);
    ownReviewPaneController.setOnDeleteReviewListener(this::onDeleteReview);
    ownReviewPaneController.setOnCancelListener(this::onCancelReview);
    ownReviewPaneController.setOnSendReviewListener(review -> onSendReviewListener.accept(review));

    // Prevent flickering
    createReviewButton.managedProperty().bind(createReviewButton.visibleProperty());
    createReviewButton.setVisible(false);

    ownReviewLabel.managedProperty().bind(ownReviewLabel.visibleProperty());
    ownReviewLabel.setVisible(false);

    pageLeftButton.managedProperty().bind(pageLeftButton.visibleProperty());
    reviewsPagination.managedProperty().bind(reviewsPagination.visibleProperty());
    pageRightButton.managedProperty().bind(pageRightButton.visibleProperty());
  }

  private void onDeleteReview(Review review) {
    Optional.ofNullable(this.onDeleteReviewListener).ifPresent(listener -> listener.accept(review));
  }

  @Override
  public Pane getRoot() {
    return reviewsRoot;
  }

  public void onCreateReviewButtonClicked() {
    ownReviewRoot.setVisible(true);
    createReviewButton.setVisible(false);
    ownReviewPaneController.setReview(Optional.empty());
  }

  private void onCancelReview() {
    setOwnReview(this.ownReview);
  }

  public void setOnSendReviewListener(Consumer<Review> onSendReviewListener) {
    this.onSendReviewListener = onSendReviewListener;
  }

  public void setReviewsSupplier(ReviewsSupplier supplier) {
    this.supplier = supplier;

    refresh();
  }

  private void refresh() {
    displayReviewsPage(currentReviewPage);

    supplier.getReviewsSummary().thenAccept(reviewsSummary -> {
      Integer reviews = reviewsSummary.map(ReviewsSummary::getReviews).orElse(0);
      numberOfReviews.set(reviews);

      // FIXME in order to calculate this properly, we need to know how many non-empty reviews there are
      reviewsPagination.setVisible(reviews > REVIEWS_PER_PAGE);

      JavaFxUtil.runLater(() -> updateRating(reviewsSummary));
    });
  }

  public void setCanWriteReview(boolean canWriteReview) {
    createReviewButton.setVisible(canWriteReview);
  }

  private void displayReviewsPage(int page) {
    supplier.getNonEmptyReviews(REVIEWS_PER_PAGE, page).thenAccept(reviews -> Platform.runLater(() -> {
      pageLeftButton.setVisible(page > 0);
      // Poor man's paging; assume there are more reviews even though there might not be. To properly implement this,
      // elide's paging metadata also needs to be returned by ApiAccessor and FafService.
      pageRightButton.setVisible(reviews.size() >= REVIEWS_PER_PAGE);

      List<Pane> reviewNodes = reviews.stream()
        .map(review -> {
          ReviewController controller = uiService.loadFxml("theme/vault/review/review.fxml");
          controller.setReview(Optional.of(review));
          return controller.getRoot();
        })
        .collect(Collectors.toList());

      otherReviewsContainer.getChildren().setAll(reviewNodes);
    }));
  }

  private void updateRating(Optional<ReviewsSummary> reviewsSummaryOptional) {
    JavaFxUtil.assertApplicationThread();

    float fiveStarsPercentage;
    float fourStarsPercentage;
    float threeStarsPercentage;
    float twoStarsPercentage;
    float oneStarPercentage;
    float average;
    int totalReviews;

    if (reviewsSummaryOptional.isPresent()) {
      ReviewsSummary reviewsSummary = reviewsSummaryOptional.get();
      Map<Byte, Integer> ratingOccurrences = reviewsSummary.getScoreCounts();

      Integer fiveStars = ratingOccurrences.getOrDefault((byte) 5, 0);
      Integer fourStars = ratingOccurrences.getOrDefault((byte) 4, 0);
      Integer threeStars = ratingOccurrences.getOrDefault((byte) 3, 0);
      Integer twoStars = ratingOccurrences.getOrDefault((byte) 2, 0);
      Integer oneStars = ratingOccurrences.getOrDefault((byte) 1, 0);

      totalReviews = Math.max(reviewsSummary.getReviews(), 1);
      fiveStarsPercentage = (float) fiveStars / totalReviews;
      fourStarsPercentage = (float) fourStars / totalReviews;
      threeStarsPercentage = (float) threeStars / totalReviews;
      twoStarsPercentage = (float) twoStars / totalReviews;
      oneStarPercentage = (float) oneStars / totalReviews;

      average = (float) reviewsSummary.getScore() / totalReviews;
    } else {
      fiveStarsPercentage = 0;
      fourStarsPercentage = 0;
      threeStarsPercentage = 0;
      twoStarsPercentage = 0;
      oneStarPercentage = 0;
      average = 0;
      totalReviews = 0;
    }

    // So that the bars' parents have their sizes
    reviewsRoot.applyCss();
    reviewsRoot.layout();

    totalReviewsLabel.setText(i18n.get("reviews.totalReviewers", totalReviews));
    fiveStarsBar.prefWidthProperty().bind(((Pane) fiveStarsBar.getParent()).widthProperty().multiply(fiveStarsPercentage));
    fourStarsBar.prefWidthProperty().bind(((Pane) fourStarsBar.getParent()).widthProperty().multiply(fourStarsPercentage));
    threeStarsBar.prefWidthProperty().bind(((Pane) threeStarsBar.getParent()).widthProperty().multiply(threeStarsPercentage));
    twoStarsBar.prefWidthProperty().bind(((Pane) twoStarsBar.getParent()).widthProperty().multiply(twoStarsPercentage));
    oneStarBar.prefWidthProperty().bind(((Pane) oneStarBar.getParent()).widthProperty().multiply(oneStarPercentage));

    scoreLabel.setText(i18n.rounded(average, 1));
    averageStarsController.setValue(average);
  }

  public void setOwnReview(Optional<Review> ownReview) {
    this.ownReview = ownReview;
    Platform.runLater(() -> {
      if (ownReview.isPresent()) {
        ownReviewPaneController.setReview(ownReview);
        ownReviewRoot.setVisible(true);
        createReviewButton.setVisible(false);
        ownReviewLabel.setVisible(true);
      } else {
        ownReviewRoot.setVisible(false);
        createReviewButton.setVisible(true);
        ownReviewLabel.setVisible(false);
      }
    });
  }

  public void setOnDeleteReviewListener(Consumer<Review> onDeleteReviewListener) {
    this.onDeleteReviewListener = onDeleteReviewListener;
  }

  public void onPageLeftButtonClicked() {
    displayReviewsPage(--currentReviewPage);
  }

  public void onPageRightButtonClicked() {
    displayReviewsPage(++currentReviewPage);
  }

  public interface ReviewsSupplier {

    CompletionStage<List<Review>> getNonEmptyReviews(int count, int page);

    CompletionStage<Optional<ReviewsSummary>> getReviewsSummary();
  }
}
