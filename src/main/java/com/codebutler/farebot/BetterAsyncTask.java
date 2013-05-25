/*
 * BetterAsyncTask.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ProgressBar;

public abstract class BetterAsyncTask<Result> extends AsyncTask<Void, ProgressBar, BetterAsyncTask.TaskResult<Result>> {
    private static final String TAG = "GliphTask";

    private ProgressDialog mProgressDialog;
    private Activity       mActivity;
    private boolean        mFinishOnError;

    public BetterAsyncTask(Activity activity) {
        this(activity, true);
    }

    public BetterAsyncTask(Activity activity, boolean showLoading) {
        this(activity, showLoading, null, false);
    }

    public BetterAsyncTask(Activity activity, boolean showLoading, boolean finishOnError) {
        this(activity, showLoading, null, finishOnError);
    }

    public BetterAsyncTask(Activity activity, boolean showLoading, int loadingText) {
        this(activity, showLoading, activity.getString(loadingText));
    }

    public BetterAsyncTask(Activity activity, boolean showLoading, String loadingText) {
        this(activity, showLoading, loadingText, false);
    }

    public BetterAsyncTask(Activity activity, boolean showLoading, String loadingText, boolean finishOnError) {
        mActivity = activity;
        mFinishOnError = finishOnError;

        if (showLoading) {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            setLoadingText(loadingText);
        }
    }

    public void cancelIfRunning() {
        if (getStatus() != Status.FINISHED) {
            super.cancel(true);
        }

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    protected void setLoadingText(String text) {
        if (mProgressDialog == null) {
            return;
        }
        mProgressDialog.setMessage(TextUtils.isEmpty(text) ? mActivity.getString(R.string.loading) : text);
    }

    @Override
    protected final TaskResult<Result> doInBackground(Void... unused) {
        try {
            return new TaskResult<Result>(doInBackground());
        } catch (Exception e) {
            Log.e(TAG, "Error in task:", e);
            return new TaskResult<Result>(e);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mProgressDialog != null)
            mProgressDialog.show();
    }

    @Override
    protected final void onPostExecute(TaskResult<Result> result) {
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
        if (!result.success()) {
            onError(result.getException());
            return;
        }

        onResult(result.getObject());
    }

    protected void onError(Exception ex) {
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
            .setTitle(R.string.error)
            .setMessage(ex.toString())
            .setPositiveButton(android.R.string.ok, null)
            .create();
        if (mFinishOnError) {
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    mActivity.finish();
                }
            });
        }
        dialog.show();
    }

    protected abstract Result doInBackground() throws Exception;
    protected abstract void onResult(Result result);

    public static class TaskResult<T> {
        private final T mObject;
        private final Exception mException;

        public TaskResult(T object) {
            mObject    = object;
            mException = null;
        }

        public TaskResult(Exception exception) {
            if (exception == null) {
                throw new IllegalArgumentException("exception may not be null");
            }

            mException = exception;
            mObject    = null;
        }

        public T getObject() {
            return mObject;
        }

        public Exception getException() {
            return mException;
        }

        public boolean success() {
            return (mException == null);
        }
    }
}