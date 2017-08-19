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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class HttpDownloader extends Downloader{

    private static Logger logger = Logger.getLogger(HttpDownloader.class.getName());

    public HttpDownloader(URL url, String outputFolder, int numConnections) {
		super(url, outputFolder, numConnections);
		download();
	}
	
	private void error() {
		logger.severe("ERROR");
		setState(ERROR);
	}


	private HttpURLConnection createAndCheckConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection)mURL.openConnection();
        conn.setConnectTimeout(10000);
        conn.connect();

        // Make sure the response code is in the 200 range.
        if (conn.getResponseCode() / 100 != 2) {
            error();
        }
        return conn;
    }

    private void checkForValidContentLength(HttpURLConnection conn){
        int contentLength = conn.getContentLength();
        if (contentLength < 1) {
            error();
        }

        if (mFileSize == -1) {
            mFileSize = contentLength;
            stateChanged();
            logger.finest("File size:" + mFileName);
        }
    }
	
	@Override
	public void run() {
		HttpURLConnection conn = null;
		try {
            /**
             * Opens connection to URL and Connects to the server
             * then checks the StatusCode
             */
            conn = createAndCheckConnection();

            // Check for valid content length.
            checkForValidContentLength(conn);
               
            // if the state is DOWNLOADING (no error) -> start downloading
            if (mState == DOWNLOADING) {
            	// check whether we have list of download threads or not, if not -> init download
            	if (mListDownloadThread.size() == 0) {
            		if (mFileSize > MIN_DOWNLOAD_SIZE) {
		                // downloading size for each thread
						int partSize = Math.round(((float)mFileSize / mNumConnections) / BLOCK_SIZE) * BLOCK_SIZE;
                        logger.finest("Part size: " + partSize);
						
						// start/end Byte for each thread
						int startByte = 0;
						int endByte = partSize - 1;
						HttpDownloadThread aThread = new HttpDownloadThread(1, mURL,
                                mOutputFolder + mFileName, startByte, endByte);
						mListDownloadThread.add(aThread);
						int i = 2;
						while (endByte < mFileSize) {
							startByte = endByte + 1;
							endByte += partSize;
							aThread = new HttpDownloadThread(i, mURL, mOutputFolder + mFileName, startByte, endByte);
							mListDownloadThread.add(aThread);
							++i;
						}
            		} else {
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

        /**
         * Downloads that part of the remote resource which is assigned to this thread
         * @throws IOException
         */
		private void downloadResourcePartially() throws IOException {
            HttpURLConnection conn = (HttpURLConnection)mURL.openConnection();
            String byteRange = mStartByte + "-" + mEndByte;
            conn.setRequestProperty("Range", "bytes=" + byteRange);
            logger.finest("bytes=" + byteRange);
            conn.connect();
            if (conn.getResponseCode() / 100 != 2) {
                error();
            }
            try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                 RandomAccessFile raf = new RandomAccessFile(mOutputFile, "rw");) {
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
            }
        }

		@Override
		public void run() {
			try  {
			    downloadResourcePartially();
			} catch (IOException e) {
				error();
			}
            logger.finest("End thread " + mThreadID);
		}
	}
}
