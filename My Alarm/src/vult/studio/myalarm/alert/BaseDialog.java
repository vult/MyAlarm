package vult.studio.myalarm.alert;

import vult.studio.myalarm.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;

/**
 * This class provides common variables and methods that are used for all
 * Dialog.
 * 
 * @author ChinhNguyen
 * 
 */
public class BaseDialog extends Dialog {

	/**
	 * A dialog showing a progress indicator and an optional text message or
	 * view.
	 */
	private ProgressDialog mProgressDialog;

	/** Interface to global information about an application environment. */
	protected Context mContext;

	public BaseDialog(Context context) {
		super(context);
		mContext = context;
		setCanceledOnTouchOutside(false);
	}

	/**
	 * show progress dialog.
	 * 
	 * @param msgResId
	 */
	protected void showProgress(int msgResId) {
		try {

			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				return;
			}
			mProgressDialog = new ProgressDialog(mContext);
			
			mProgressDialog.setIndeterminate(true);
			if (msgResId != 0) {
				mProgressDialog.setMessage(mContext.getString(msgResId));
			}

			mProgressDialog.setCancelable(false);

			mProgressDialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * cancel progress dialog.
	 */
	protected void dismissProgress() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	/**
	 * Show Error Dialog
	 * 
	 * @param msgResId
	 *            the string resource's id
	 */
	protected void showErrorDialog(int msgResId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(mContext.getString(msgResId));
		builder.setPositiveButton(mContext.getString(R.string.ok), null);
		builder.show();
	}

	/**
	 * Show Error Dialog
	 * 
	 * @param msg
	 *            the message to show
	 */
	protected void showErrorDialog(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(msg);
		builder.setPositiveButton(mContext.getString(R.string.ok), null);
		builder.show();
	}
}
