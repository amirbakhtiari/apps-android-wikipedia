package org.wikipedia.readinglist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AddToReadingListDialog extends ExtendedBottomSheetDialogFragment {
    public enum InvokeSource implements EnumCode {
        BOOKMARK_BUTTON(0),
        CONTEXT_MENU(1),
        LINK_PREVIEW_MENU(2),
        PAGE_OVERFLOW_MENU(3),
        FEED(4),
        NEWS_ACTIVITY(5),
        READING_LIST_ACTIVITY(6),
        MOST_READ_ACTIVITY(7),
        RANDOM_ACTIVITY(8),
        ON_THIS_DAY_ACTIVITY(9),
        READ_MORE_BOOKMARK_BUTTON(10);

        private static final EnumCodeMap<InvokeSource> MAP = new EnumCodeMap<>(InvokeSource.class);

        private final int code;

        public static InvokeSource of(int code) {
            return MAP.get(code);
        }

        @Override public int code() {
            return code;
        }

        InvokeSource(int code) {
            this.code = code;
        }
    }

    private List<PageTitle> titles;
    private ReadingListAdapter adapter;
    private View listsContainer;
    private View onboardingContainer;
    private View onboardingButton;
    private InvokeSource invokeSource;
    private CreateButtonClickListener createClickListener = new CreateButtonClickListener();
    private CompositeDisposable disposables = new CompositeDisposable();

    private List<ReadingList> readingLists = new ArrayList<>();

    @Nullable private DialogInterface.OnDismissListener dismissListener;
    private ReadingListItemCallback listItemCallback = new ReadingListItemCallback();

    public static AddToReadingListDialog newInstance(@NonNull PageTitle title, InvokeSource source) {
        return newInstance(title, source, null);
    }

    public static AddToReadingListDialog newInstance(@NonNull PageTitle title, InvokeSource source,
                                                     @Nullable DialogInterface.OnDismissListener listener) {
        return newInstance(Collections.singletonList(title), source, listener);
    }

    public static AddToReadingListDialog newInstance(@NonNull List<PageTitle> titles, InvokeSource source) {
        return newInstance(titles, source, null);
    }

    public static AddToReadingListDialog newInstance(@NonNull List<PageTitle> titles, InvokeSource source,
                                                     @Nullable DialogInterface.OnDismissListener listener) {
        AddToReadingListDialog dialog = new AddToReadingListDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList("titles", new ArrayList<Parcelable>(titles));
        args.putInt("source", source.code());
        dialog.setArguments(args);
        dialog.setOnDismissListener(listener);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        titles = getArguments().getParcelableArrayList("titles");
        invokeSource = InvokeSource.of(getArguments().getInt("source"));
        adapter = new ReadingListAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_add_to_reading_list, container);

        listsContainer = rootView.findViewById(R.id.lists_container);
        onboardingContainer = rootView.findViewById(R.id.onboarding_container);
        onboardingButton = rootView.findViewById(R.id.onboarding_button);

        RecyclerView readingListView = rootView.findViewById(R.id.list_of_lists);
        readingListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        readingListView.setAdapter(adapter);

        View createButton = rootView.findViewById(R.id.create_button);
        createButton.setOnClickListener(createClickListener);

        if (savedInstanceState == null) {
            // Log a click event, but only the first time the dialog is shown.
            new ReadingListsFunnel().logAddClick(invokeSource);
        }

        onboardingContainer.setVisibility(View.GONE);
        listsContainer.setVisibility(View.GONE);
        updateLists();
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior.from((View) getView().getParent()).setPeekHeight(DimenUtil
                .roundedDpToPx(DimenUtil.getDimension(R.dimen.readingListSheetPeekHeight)));
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (dismissListener != null) {
            dismissListener.onDismiss(null);
        }
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        dismissListener = listener;
    }

    private void checkAndShowOnboarding() {
        boolean isOnboarding = Prefs.isReadingListTutorialEnabled();
        if (isOnboarding) {
            // Don't show onboarding message if the user already has items in lists (i.e. from syncing).
            for (ReadingList list : readingLists) {
                if (!list.pages().isEmpty()) {
                    isOnboarding = false;
                    Prefs.setReadingListTutorialEnabled(false);
                    break;
                }
            }
        }
        onboardingButton.setOnClickListener((v) -> {
            onboardingContainer.setVisibility(View.GONE);
            listsContainer.setVisibility(View.VISIBLE);
            Prefs.setReadingListTutorialEnabled(false);
            if (readingLists.isEmpty()) {
                showCreateListDialog();
            }
        });
        listsContainer.setVisibility(isOnboarding ? View.GONE : View.VISIBLE);
        onboardingContainer.setVisibility(isOnboarding ? View.VISIBLE : View.GONE);
    }

    private void updateLists() {
        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().getAllLists())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lists -> {
                    readingLists = lists;
                    ReadingList.sort(readingLists, Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC));
                    adapter.notifyDataSetChanged();
                    checkAndShowOnboarding();
                }, L::w));
    }

    private class CreateButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (readingLists.size() >= Constants.MAX_READING_LISTS_LIMIT) {
                String message = getString(R.string.reading_lists_limit_message);
                dismiss();
                FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            } else {
                showCreateListDialog();
            }
        }
    }

    private void showCreateListDialog() {
        List<String> existingTitles = new ArrayList<>();
        for (ReadingList tempList : readingLists) {
            existingTitles.add(tempList.title());
        }
        ReadingListTitleDialog.readingListTitleDialog(requireContext(), "", "",
                existingTitles, (text, description) -> {
                    ReadingList list = ReadingListDbHelper.instance().createList(text, description);
                    addAndDismiss(list, titles);
                }).show();
    }

    private void addAndDismiss(final ReadingList readingList, final PageTitle title) {

        if (readingList.pages().size() >= SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_article_limit_message, readingList.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            dismiss();
            return;
        }

        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().pageExistsInList(readingList, title))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(exists -> {
                    String message;
                    if (exists) {
                        message = getString(R.string.reading_list_already_exists);
                        showViewListSnackBar(readingList, message);

                    } else {
                        message = String.format(getString(R.string.reading_list_added_to_named), readingList.title());
                        new ReadingListsFunnel(title.getWikiSite()).logAddToList(readingList, readingLists.size(), invokeSource);

                        ReadingListDbHelper.instance().addPageToList(readingList, title, true);
                        showViewListSnackBar(readingList, message);

                    }
                    dismiss();
                }, L::w));
    }

    private void addAndDismiss(final ReadingList readingList, final List<PageTitle> titles) {

        if ((readingList.pages().size() + titles.size()) > SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_article_limit_message, readingList.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            dismiss();
            return;
        }

        if (titles.size() == 1) {
            addAndDismiss(readingList, titles.get(0));
            return;
        }

        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().addPagesToListIfNotExist(readingList, titles))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(numAdded -> {
                    String message;
                    if (numAdded == 0) {
                        message = getString(R.string.reading_list_already_contains_selection);
                    } else {
                        message = String.format(getString(R.string.reading_list_added_articles_list_titled), numAdded,
                                readingList.title());
                        new ReadingListsFunnel().logAddToList(readingList, readingLists.size(), invokeSource);
                    }
                    showViewListSnackBar(readingList, message);
                    dismiss();
                }, L::w));
    }

    private void showViewListSnackBar(@NonNull final ReadingList list, @NonNull String message) {
        FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT)
                .setAction(R.string.reading_list_added_view_button, v -> v.getContext().startActivity(ReadingListActivity.newIntent(v.getContext(), list))).show();
    }

    private class ReadingListItemCallback implements ReadingListItemView.Callback {
        @Override
        public void onClick(@NonNull ReadingList readingList) {
            addAndDismiss(readingList, titles);
        }

        @Override
        public void onRename(@NonNull ReadingList readingList) {
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
        }

        @Override
        public void onSaveAllOffline(@NonNull ReadingList readingList) {
        }

        @Override
        public void onRemoveAllOffline(@NonNull ReadingList readingList) {
        }
    }

    private class ReadingListItemHolder extends RecyclerView.ViewHolder {
        private ReadingListItemView itemView;

        ReadingListItemHolder(ReadingListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setOverflowButtonVisible(false);
        }

        void bindItem(ReadingList readingList) {
            itemView.setReadingList(readingList, ReadingListItemView.Description.SUMMARY);
        }

        public ReadingListItemView getView() {
            return itemView;
        }
    }

    private final class ReadingListAdapter extends RecyclerView.Adapter<ReadingListItemHolder> {
        @Override
        public int getItemCount() {
            return readingLists.size();
        }

        @Override
        public ReadingListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int pos) {
            ReadingListItemView view = new ReadingListItemView(getContext());
            return new ReadingListItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReadingListItemHolder holder, int pos) {
            holder.bindItem(readingLists.get(pos));
        }

        @Override public void onViewAttachedToWindow(@NonNull ReadingListItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(listItemCallback);
        }

        @Override public void onViewDetachedFromWindow(@NonNull ReadingListItemHolder holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }
}
