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

import java.util.Observable;
import java.util.Observer;

import javax.swing.JProgressBar;
import javax.swing.table.AbstractTableModel;

/**
 *  This class manages the download table's data.
 *
 */
public class DownloadTableModel extends AbstractTableModel implements Observer {
    
	private static final long serialVersionUID = -7852567715605820609L;

	// These are the names for the table's columns.
    private static final String[] columnNames = {"URL", "Size (KB)",
    "Progress", "Status", "Destination"};
    
    // These are the classes for each column's values.
    @SuppressWarnings("rawtypes")
	private static final Class[] columnClasses = {String.class,
    	String.class, JProgressBar.class, String.class, String.class};
    
    /**
     *  Add a new download to the table.
     */
    public void addNewDownload(Downloader download) {
        // Register to be notified when the download changes.
        download.addObserver(this);
        
        // Fire table row insertion notification to table.
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }
    
    
    /**
     *  Remove a download from the list.
     */
    public void clearDownload(int row) {        
        // Fire table row deletion notification to table.
        fireTableRowsDeleted(row, row);
    }
    
    /**
     *  Get table's column count.
     */
    public int getColumnCount() {
        return columnNames.length;
    }
    
    /**
     *  Get a column's name.
     */
    public String getColumnName(int col) {
        return columnNames[col];
    }
    
    /**
     *  Get a column's class.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Class getColumnClass(int col) {
        return columnClasses[col];
    }
    
    /**
     *  Get table's row count.
     */
    public int getRowCount() {
        return DownloadManager.getInstance().getDownloadList().size();
    }
    
    /**
     *  Get value for a specific row and column combination.
     */
    public Object getValueAt(int row, int col) {
    	// Get download from download list
        Downloader download = DownloadManager.getInstance().getDownloadList().get(row);
        
        switch (col) {
            case 0: // URL
                return download.getURL();
            case 1: // Size
                int size = download.getFileSize();
                return (size == -1) ? "" : (Integer.toString(size/1000));
            case 2: // Progress
                return new Float(download.getProgress());
            case 3: // Status
                return Downloader.STATUSES[download.getState()];
            case 4 : //Destination
            {
                int state = download.getState();
                if(state == 2 ) return download.getOutputFolder() + download.getFileName();
                else return "-";
            }

        }
        return "";
    }
    
    /**
     * Update is called when a Download notifies its observers of any changes
     */
    public void update(Observable o, Object arg) {
        int index = DownloadManager.getInstance().getDownloadList().indexOf(o);
        
        // Fire table row update notification to table.
        fireTableRowsUpdated(index, index);
    }
}