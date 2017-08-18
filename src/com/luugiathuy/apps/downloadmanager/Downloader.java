/**
Copyright (c) 2011-present - Luu Gia Thuy

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
*/

package com.luugiathuy.apps.downloadmanager;

import java.net.URL;
import java.util.ArrayList;
import java.util.Observable;
import java.util.logging.Logger;

public abstract class Downloader extends Observable implements Runnable{

	private static Logger logger = Logger.getLogger(Downloader.class.getName());


	// Member variables
	/** The URL to download the file */
	protected URL mURL;
	
	/** Output folder for downloaded file */
	protected String mOutputFolder;
	
	/** Number of connections (threads) to download the file */
	protected int mNumConnections;
	
	/** The file name, extracted from URL */
	protected String mFileName;
	
	/** Size of the downloaded file (in bytes) */
	protected int mFileSize;
	
	/** The state of the download */
	protected int mState;
	
	/** downloaded size of the file (in bytes) */
	protected int mDownloaded;
	
	/** List of download threads */
	protected ArrayList<DownloadThread> mListDownloadThread;
	
	// Contants for block and buffer size
	protected static final int BLOCK_SIZE = 4096;
	protected static final int BUFFER_SIZE = 4096;
	protected static final int MIN_DOWNLOAD_SIZE = BLOCK_SIZE * 100;
	
	// These are the status names.
    public static final String STATUSES[] = {"Downloading",
    				"Paused", "Complete", "Cancelled", "Error"};
	
	// Contants for download's state
	public static final int DOWNLOADING = 0;
	public static final int PAUSED = 1;
	public static final int COMPLETED = 2;
	public static final int CANCELLED = 3;
	public static final int ERROR = 4;
	
	/**
	 * Constructor
	 * @param url
	 * @param outputFolder
	 * @param numConnections
	 */
	protected Downloader(URL url, String outputFolder, int numConnections) {
		mURL = url;
		mOutputFolder = outputFolder;
		mNumConnections = numConnections;
		
		// Get the file name from url path
		String fileURL = url.getFile();
		mFileName = fileURL.substring(fileURL.lastIndexOf('/') + 1);
		logger.finest("File name:" + mFileName);
		mFileSize = -1;
		mState = DOWNLOADING;
		mDownloaded = 0;
		
		mListDownloadThread = new ArrayList<>();
	}
	
	/**
	 * Pause the downloader
	 */
	public void pause() {
		setState(PAUSED);
	}
	
	/**
	 * Resume the downloader
	 */
	public void resume() {
		setState(DOWNLOADING);
		download();
	}
	
	/**
	 * Cancel the downloader
	 */
	public void cancel() {
		setState(CANCELLED);
	}
	
	/**
	 * Get the URL (in String)
	 */
	public String getURL() {
		return mURL.toString();
	}
	
	/**
	 * Get the downloaded file's size
	 */
	public int getFileSize() {
		return mFileSize;
	}
	
	/**
	 * Get the current progress of the download
	 */
	public float getProgress() {
		return ((float)mDownloaded / mFileSize) * 100;
	}
	
	/**
	 * Get current state of the downloader
	 */
	public int getState() {
		return mState;
	}
	
	/**
	 * Set the state of the downloader
	 */
	protected void setState(int value) {
		mState = value;
		stateChanged();
	}
	
	/**
	 * Start or resume download
	 */
	protected void download() {
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * Increase the downloaded size
	 */
	protected synchronized void downloaded(int value) {
		mDownloaded += value;
		stateChanged();
	}
	
	/**
	 * Set the state has changed and notify the observers
	 */
	protected void stateChanged() {
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Thread to download part of a file
	 */
	protected abstract class DownloadThread implements Runnable {
		protected int mThreadID;
		protected URL mURL;
		protected String mOutputFile;
		protected int mStartByte;
		protected int mEndByte;
		protected boolean mIsFinished;
		protected Thread mThread;
		
		public DownloadThread(int threadID, URL url, String outputFile, int startByte, int endByte) {
			mThreadID = threadID;
			mURL = url;
			mOutputFile = outputFile;
			mStartByte = startByte;
			mEndByte = endByte;
			mIsFinished = false;
			
			download();
		}
		
		/**
		 * Get whether the thread is finished download the part of file
		 */
		public boolean isFinished() {
			return mIsFinished;
		}
		
		/**
		 * Start or resume the download
		 */
		public void download() {
			mThread = new Thread(this);
			mThread.start();
		}
		
		/**
		 * Waiting for the thread to finish
		 * @throws InterruptedException
		 */
		public void waitFinish() throws InterruptedException {
			mThread.join();			
		}
		
	}
}
