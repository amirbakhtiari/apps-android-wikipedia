package org.wikipedia.feed.onboarding;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.announcement.Announcement;
import org.wikipedia.feed.model.CardType;

import androidx.annotation.NonNull;

public class CustomizeOnboardingCard extends OnboardingCard {
    public CustomizeOnboardingCard(@NonNull Announcement announcement) {
        super(announcement);
    }

    @NonNull @Override public CardType type() {
        return CardType.ONBOARDING_CUSTOMIZE_FEED;
    }

    public boolean shouldShow() {
        return super.shouldShow() && WikipediaApp.getInstance().isOnline();
    }

    @Override public int prefKey() {
        return R.string.preference_key_feed_customize_onboarding_card_enabled;
    }
}
