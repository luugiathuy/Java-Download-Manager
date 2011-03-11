package com.luugiathuy.apps.downloadmanager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpDownloader extends Downloader{
	
	public HttpDownloader(URL url, String outputFolder, int numConnections) {
		super(url, outputFolder, numConnections);
		download();
	}
	
	private void error() {
		System.out.println("ERROR");
		setState(ERROR);
	}
	
	@Override
	public void run() {
		HttpURLConnection conn = null;
		try {
			// Open connection to URL
			conn = (HttpURLConnection)mURL.openConnection();
			conn.setConnectTimeout(10000);
			
			// Connect to server
			conn.connect();
			
			// Make sure the response code is in the 200 range.
            if (conn.getResponseCode() / 100 != 2) {
                error();
            }
            
            // Check for valid content length.
            int contentLength = conn.getContentLength();
            if (contentLength < 1) {
                error();
            }
			
            if (mFileSize == -1) {
            	mFileSize = contentLength;
            	stateChanged();
            	System.out.println("File size: " + mFileSize);
            }
               
            // if the state is DOWNLOADING (no error) -> start downloading
            if (mState == DOWNLOADING) {
            	// check whether we have list of download threads or not, if not -> init download
            	if (mListDownloadThread.size() == 0)
            	{  
            		if (mFileSize > MIN_DOWNLOAD_SIZE) {
		                // downloading size for each thread
						int partSize = Math.round(((float)mFileSize / mNumConnections) / BLOCK_SIZE) * BLOCK_SIZE;
						System.out.println("Part size: " + partSize);
						
						// start/end Byte for each thread
						int startByte = 0;
						int endByte = partSize - 1;
						HttpDownloadThread aThread = new HttpDownloadThread(1, mURL, mOutputFolder + mFileName, startByte, endByte);
						mListDownloadThread.add(aThread);
						int i = 2;
						while (endByte < mFileSize) {
							startByte = endByte + 1;
							endByte += partSize;
							aThread = new HttpDownloadThread(i, mURL, mOutputFolder + mFileName, startByte, endByte);
							mListDownloadThread.add(aThread);
							++i;
						}
            		} else
            		{
            			HttpDownloadThread aThread = new HttpDownloadThread(1, mURL, mOutputFolder + mFileName, 0, mFileSize);
						mListDownloadThread.add(aThread);
            		}
            	} else { // resume all downloading threads
            		for (int i=0; i<mListDownloadThread.size(); ++i) {
            			if (!mListDownloadThread.get(i).isFinished())
            				mListDownloadThread.get(i).download();
            		}
            	}
				
				// waiting for all threads to complete
				for (int i=0; i<mListDownloadThread.size(); ++i) {
					mListDownloadThread.get(i).waitFinish();
				}
				
				// check the current state again
				if (mState == DOWNLOADING) {
					setState(COMPLETED);
				}
            }
		} catch (Exception e) {
			error();
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}
	
	/**
	 * Thread using Http protocol to download a part of file
	 */
	private class HttpDownloadThread extends DownloadThread {
		
		/**
		 * Constructor
		 * @param threadID
		 * @param url
		 * @param outputFile
		 * @param startByte
		 * @param endByte
		 */
		public HttpDownloadThread(int threadID, URL url, String outputFile, int startByte, int endByte) {
			super(threadID, url, outputFile, startByte, endByte);
		}

		@Override
		public void run() {
			BufferedInputStream in = null;
			RandomAccessFile raf = null;
			
			try {
				// open Http connection to URL
				HttpURLConnection conn = (HttpURLConnection)mURL.openConnection();
				
				// set the range of byte to download
				String byteRange = mStartByte + "-" + mEndByte;
				conn.setRequestProperty("Range", "bytes=" + byteRange);
				System.out.println("bytes=" + byteRange);
				
				// connect to server
				conn.connect();
				
				// get the input stream
				in = new BufferedInputStream(conn.getInputStream());
				
				// open the output file and seek to the start location
				raf = new RandomAccessFile(mOutputFile, "rw");
				raf.seek(mStartByte);
				
				byte data[] = new byte[BUFFER_SIZE];
				int numRead;
				while((mState == DOWNLOADING) && ((numRead = in.read(data,0,BUFFER_SIZE)) != -1))
				{
					// write to buffer
					raf.write(data,0,numRead);
					// increase the startByte for resume later
					mStartByte += numRead;
					// increase the downloaded size
					downloaded(numRead);
				}
				
				if (mState == DOWNLOADING) {
					mIsFinished = true;
				}
			} catch (IOException e) {
				error();
			} finally {
				if (raf != null) {
					try {
						raf.close();
					} catch (IOException e) {}
				}
				
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {}
				}
			}
			
			System.out.println("End thread " + mThreadID);
		}
	}
}
