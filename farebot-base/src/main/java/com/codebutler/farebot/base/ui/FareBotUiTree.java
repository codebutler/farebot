package com.codebutler.farebot.base.ui;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class FareBotUiTree {

    public abstract List<Item> getItems();

    public static Builder builder(Context context) {
        return new AutoValue_FareBotUiTree.Builder(context);
    }

    private static List<Item> buildItems(List<Item.Builder> itemBuilders) {
        ImmutableList.Builder<Item> itemsBuilder = new ImmutableList.Builder<>();
        for (Item.Builder builder : itemBuilders) {
            itemsBuilder.add(builder.build());
        }
        return itemsBuilder.build();
    }

    public static class Builder {

        private final List<Item.Builder> mItemBuilders = new ArrayList<>();

        private final Context mContext;

        private Builder(Context context) {
            mContext = context;
        }

        public Item.Builder item() {
            Item.Builder builder = Item.builder(mContext);
            mItemBuilders.add(builder);
            return builder;
        }

        public FareBotUiTree build() {
            return new AutoValue_FareBotUiTree(buildItems(mItemBuilders));
        }
    }

    @AutoValue
    public abstract static class Item {

        public abstract String getTitle();

        @Nullable
        public abstract Object getValue();

        public abstract List<Item> children();

        public static Builder builder(Context context) {
            return new AutoValue_FareBotUiTree_Item.Builder(context);
        }

        public static class Builder {
            private String mTitle;
            private Object mValue;

            private final List<Item.Builder> mChildBuilders = new ArrayList<>();

            private final Context mContext;

            private Builder(Context context) {
                mContext = context;
            }

            public Builder title(String text) {
                mTitle = text;
                return this;
            }

            public Builder title(@StringRes int textResId) {
                return title(mContext.getString(textResId));
            }

            public Builder value(Object value) {
                mValue = value;
                return this;
            }

            public Item.Builder item() {
                Builder builder = Item.builder(mContext);
                mChildBuilders.add(builder);
                return builder;
            }

            public Item.Builder item(String title, Object value) {
                return item()
                        .title(title)
                        .value(value);
            }

            public Item.Builder item(@StringRes int title, Object value) {
                return item(mContext.getString(title), value);
            }

            public Item build() {
                return new AutoValue_FareBotUiTree_Item(mTitle, mValue, buildItems(mChildBuilders));
            }
        }
    }
}

