/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.Contract.notNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.LogUtil;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.servermanager.AddeThread.McservEvent;
import edu.wisc.ssec.mcidasv.util.McVTextField.Prompt;

/**
 * This class is the GUI frontend to {@link EntryStore} (the server manager).
 * It allows users to manipulate their local and remote ADDE data.
 */
// TODO(jon): don't forget to persist tab choice and window position. maybe also the "positions" of the scrollpanes (if possible).
// TODO(jon): GUI could look much better.
// TODO(jon): finish up the javadocs.
@SuppressWarnings("serial")
public class TabbedAddeManager extends javax.swing.JFrame {

    /** Pretty typical logger object. */
    private final static Logger logger = LoggerFactory.getLogger(TabbedAddeManager.class);

    /** Path to the help resources. */
    private static final String HELP_TOP_DIR = "/docs/userguide";

    /** Help target for the remote servers. */
    private static final String REMOTE_HELP_TARGET = "idv.tools.remotedata";

    /** Help target for the local servers. */
    private static final String LOCAL_HELP_TARGET = "idv.tools.localdata";

    /** ID used to save/restore the last visible tab between sessions. */
    private static final String LAST_TAB = "mcv.adde.lasttab";

    /** ID used to save/restore the last directory that contained a MCTABLE.TXT. */
    private static final String LAST_IMPORTED = "mcv.adde.lastmctabledir";

    /**
     * These are the various {@literal "events"} that the server manager GUI
     * supports. These are published via the wonderful {@link EventBus#publish(Object)} method.
     */
    public enum Event { 
        /** The GUI was created. */
        OPENED,
        /** The GUI was hidden/minimized/etc. */
        HIDDEN,
        /** GUI was unhidden or some such thing. */
        SHOWN,
        /** The GUI was closed. */
        CLOSED
    };

    /** Reference back to the McV god object. */
//    private final McIDASV mcv;

    /** Reference to the actual server manager. */
    private final EntryStore entryStore;

    /** The currently selected {@link RemoteAddeEntry} or {@code null} if nothing is selected. */
//    private RemoteAddeEntry selectedRemoteEntry = null;

    private final List<RemoteAddeEntry> selectedRemoteEntries;

    /** The currently selected {@link LocalAddeEntry} or {@code null} if nothing is selected. */
//    private LocalAddeEntry selectedLocalEntry = null;

    private final List<LocalAddeEntry> selectedLocalEntries;

    /**
     * Creates a standalone server manager GUI.
     */
    public TabbedAddeManager() {
        this.entryStore = null;
        this.selectedLocalEntries = arrList();
        this.selectedRemoteEntries = arrList();
        initComponents();
    }

    /**
     * Creates a server manager GUI that's linked back to the rest of McIDAS-V.
     * 
     * @param entryStore Server manager reference.
     * 
     * @throws NullPointerException if {@code entryStore} is {@code null}.
     */
    public TabbedAddeManager(final EntryStore entryStore) {
        notNull(entryStore, "Cannot pass a null server manager");
        this.entryStore = entryStore;
        this.selectedLocalEntries = arrList();
        this.selectedRemoteEntries = arrList();
        initComponents();
    }

    /**
     * If the GUI isn't shown, this method will display things. If the GUI <i>is 
     * shown</i>, bring it to the front.
     * 
     * <p>This method publishes {@link Event#SHOWN}.
     */
    public void showManager() {
        if (!isVisible()) {
            setVisible(true);
        } else {
            toFront();
        }
        EventBus.publish(Event.SHOWN);
    }

    /**
     * Closes and disposes (if needed) the GUI.
     */
    public void closeManager() {
        EventBus.publish(Event.CLOSED);
        if (isDisplayable()) {
            dispose();
        }
    }

    // TODO(jon): still needs to refresh the local table.
    protected void refreshDisplay() {
        ((RemoteAddeTableModel)remoteTable.getModel()).refreshEntries();
        ((LocalAddeTableModel)localEntries.getModel()).refreshEntries();
    }

    public void showRemoteEditor() {
        if (tabbedPane.getSelectedIndex() != 0) {
            tabbedPane.setSelectedIndex(0);
        }
        RemoteEntryEditor editor = new RemoteEntryEditor(this, true, this, entryStore);
        editor.setVisible(true);
    }

    public void showRemoteEditor(final RemoteAddeEntry entry) {
        if (tabbedPane.getSelectedIndex() != 0) {
            tabbedPane.setSelectedIndex(0);
        }
        RemoteEntryEditor editor = new RemoteEntryEditor(this, true, this, entryStore, entry);
        editor.setVisible(true);
    }

    public void removeRemoteEntries(final List<RemoteAddeEntry> entries) {
        if (entries == null) {
            return;
        }
        if (entryStore.removeEntries(entries)) {
            RemoteAddeTableModel tableModel = ((RemoteAddeTableModel)remoteTable.getModel());
            int first = Integer.MAX_VALUE;
            int last = Integer.MIN_VALUE;
            for (RemoteAddeEntry entry : entries) {
                int index = tableModel.getRowForEntry(entry);
                if (index < 0) {
                    continue;
                } else {
                    if (index < first) {
                        first = index;
                    }
                    if (index > last) {
                        last = index;
                    }
                }
            }
            tableModel.fireTableRowsDeleted(first, last);
            refreshDisplay();
            remoteTable.revalidate();
            remoteTable.setRowSelectionInterval(first, first);
        } else {
            logger.debug("removeRemoteEntries: could not remove entries={}", entries);
        }
    }

    public void showLocalEditor() {
        if (tabbedPane.getSelectedIndex() != 1) {
            tabbedPane.setSelectedIndex(1);
        }
        LocalEntryEditor editor = new LocalEntryEditor(this, true, this, entryStore);
        editor.setVisible(true);
    }

    public void showLocalEditor(final LocalAddeEntry entry) {
        if (tabbedPane.getSelectedIndex() != 1)
            tabbedPane.setSelectedIndex(1);
        LocalEntryEditor editor = new LocalEntryEditor(this, true, this, entryStore, entry);
        editor.setVisible(true);
    }

    public void removeLocalEntries(final List<LocalAddeEntry> entries) {
        if (entries == null) {
            return;
        }
        if (entryStore.removeEntries(entries)) {
            LocalAddeTableModel tableModel = ((LocalAddeTableModel)localEntries.getModel());
            int first = Integer.MAX_VALUE;
            int last = Integer.MIN_VALUE;
            for (LocalAddeEntry entry : entries) {
                int index = tableModel.getRowForEntry(entry);
                if (index < 0) {
                    continue;
                } else {
                    if (index < first) {
                        first = index;
                    }
                    if (index > last) {
                        last = index;
                    }
                }
            }
            tableModel.fireTableRowsDeleted(first, last);
            refreshDisplay();
            localEntries.revalidate();
            localEntries.setRowSelectionInterval(first, first);
        } else {
            logger.debug("removeLocalEntries: could not remove entries={}", entries);
        }
    }

    public void importMctable(final String path) {
        Set<RemoteAddeEntry> imported = EntryTransforms.extractMctableEntries(path);
        if (imported == Collections.EMPTY_SET) {
            LogUtil.userErrorMessage("Selection does not appear to a valid MCTABLE.TXT file:\n"+path);
        } else {
            // verify entries first!
            entryStore.addEntries(imported);
            refreshDisplay();
            repaint();
        }
    }

    public void restartLocalServer() {
        entryStore.restartLocalServer();
    }

    @EventSubscriber(eventClass=McservEvent.class)
    public void mcservUpdated(final McservEvent event) {
        final String msg;
        switch (event) {
            case ACTIVE:
                msg = "Local servers are already running.";
                break;
            case DIED:
                msg = "Local servers quit unexpectedly...";
                break;
            case STARTED:
                msg = "Local servers are listening on port "+EntryStore.getLocalPort();
                break;
            case STOPPED:
                msg = "Local servers have been stopped.";
                break;
            default:
                msg = "Unknown local servers status: "+event.toString();
                break;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusLabel.setText(msg);
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        ucar.unidata.ui.Help.setTopDir(HELP_TOP_DIR);

        tabbedPane = new javax.swing.JTabbedPane();
        remoteTab = new javax.swing.JPanel();
        remoteScroller = new javax.swing.JScrollPane();
        remoteTable = new javax.swing.JTable();
        actionPanel = new javax.swing.JPanel();
        newEntryButton = new javax.swing.JButton();
        editEntryButton = new javax.swing.JButton();
        removeEntryButton = new javax.swing.JButton();
        importButton = new javax.swing.JButton();
        localTab = new javax.swing.JPanel();
        localEntriesScroller = new javax.swing.JScrollPane();
        localEntries = new javax.swing.JTable();
        statusPanel = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        restartButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newRemoteItem = new javax.swing.JMenuItem();
        newLocalItem = new javax.swing.JMenuItem();
        fileSeparator1 = new javax.swing.JPopupMenu.Separator();
        closeItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        editEntryItem = new javax.swing.JMenuItem();
        removeEntryItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        remoteHelpItem = new javax.swing.JMenuItem();
        localHelpItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("ADDE Data Manager");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        remoteTable.setModel(new RemoteAddeTableModel(entryStore));
        remoteTable.setColumnSelectionAllowed(false);
        remoteTable.setRowSelectionAllowed(true);
        remoteTable.getTableHeader().setReorderingAllowed(false);
        remoteScroller.setViewportView(remoteTable);
        remoteTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        remoteTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                remoteSelectionModelChanged(e);
            }
        });
        remoteTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(final java.awt.event.MouseEvent e) {
                if ((e.getClickCount() == 2) && (hasSingleRemoteSelection())) {
                    showRemoteEditor(getSingleRemoteSelection());
                }
            }
        });

        newEntryButton.setText("Add New Dataset");
        newEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEntryButtonActionPerformed(evt);
            }
        });

        editEntryButton.setText("Edit Dataset");
        editEntryButton.setEnabled(false);
        editEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editEntryButtonActionPerformed(evt);
            }
        });

        removeEntryButton.setText("Remove Dataset");
        removeEntryButton.setEnabled(false);
        removeEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeEntryButtonActionPerformed(evt);
            }
        });

        importButton.setText("Import MCTABLE...");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout actionPanelLayout = new javax.swing.GroupLayout(actionPanel);
        actionPanel.setLayout(actionPanelLayout);
        actionPanelLayout.setHorizontalGroup(
            actionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(actionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(newEntryButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editEntryButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(removeEntryButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(importButton)
                .addContainerGap(77, Short.MAX_VALUE))
        );
        actionPanelLayout.setVerticalGroup(
            actionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(actionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(newEntryButton)
                .addComponent(editEntryButton)
                .addComponent(removeEntryButton)
                .addComponent(importButton))
        );

        javax.swing.GroupLayout remoteTabLayout = new javax.swing.GroupLayout(remoteTab);
        remoteTab.setLayout(remoteTabLayout);
        remoteTabLayout.setHorizontalGroup(
            remoteTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, remoteTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(remoteTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(remoteScroller, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                    .addComponent(actionPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        remoteTabLayout.setVerticalGroup(
            remoteTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(remoteTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(remoteScroller, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(actionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Remote Data", remoteTab);

        localEntries.setModel(new LocalAddeTableModel(entryStore));
        localEntries.setColumnSelectionAllowed(false);
        localEntries.setRowSelectionAllowed(true);
        localEntries.getTableHeader().setReorderingAllowed(false);
        localEntriesScroller.setViewportView(localEntries);
        localEntries.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        localEntries.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                localSelectionModelChanged(e);
            }
        });
        localEntries.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(final java.awt.event.MouseEvent e) {
                if ((e.getClickCount() == 2) && (hasSingleLocalSelection())) {
                    showLocalEditor(getSingleLocalSelection());
                }
            }
        });

        if (!entryStore.checkLocalServer()) {
            statusLabel.setText("Local server is not running.");
            restartButton.setText("Start Me!");
        }
        else {
            statusLabel.setText("Local server is running.");
            restartButton.setText("Restart Me!");
        }
        restartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restartButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addComponent(statusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 314, Short.MAX_VALUE)
                .addComponent(restartButton))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(restartButton)
                .addComponent(statusLabel))
        );

        javax.swing.GroupLayout localTabLayout = new javax.swing.GroupLayout(localTab);
        localTab.setLayout(localTabLayout);
        localTabLayout.setHorizontalGroup(
            localTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, localTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(localTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(localEntriesScroller, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                    .addComponent(statusPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        localTabLayout.setVerticalGroup(
            localTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(localTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(localEntriesScroller, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Local Data", localTab);

        fileMenu.setText("File");

        newRemoteItem.setText("New Remote Dataset");
        newRemoteItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newRemoteItemActionPerformed(evt);
            }
        });
        fileMenu.add(newRemoteItem);

        newLocalItem.setText("New Local Dataset");
        newLocalItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newLocalItemActionPerformed(evt);
            }
        });
        fileMenu.add(newLocalItem);
        fileMenu.add(fileSeparator1);

        closeItem.setText("Close");
        closeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeManager(evt);
            }
        });
        fileMenu.add(closeItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");
        editEntryItem.setText("Edit Entry...");
        editEntryItem.setEnabled(false);
        editEntryItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editEntryButtonActionPerformed(evt);
            }
        });

        removeEntryItem.setText("Remove Entry");
        removeEntryItem.setEnabled(false);
        removeEntryItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeEntryButtonActionPerformed(evt);
            }
        });
        editMenu.add(editEntryItem);
        editMenu.add(removeEntryItem);
        menuBar.add(editMenu);

        helpMenu.setText("Help");

        remoteHelpItem.setText("Show Remote Data Help");
        remoteHelpItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteHelpItemActionPerformed(evt);
            }
        });
        helpMenu.add(remoteHelpItem);

        localHelpItem.setText("Show Local Data Help");
        localHelpItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                localHelpItemActionPerformed(evt);
            }
        });
        helpMenu.add(localHelpItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE))
        );

        tabbedPane.setSelectedIndex(getLastTab());
        tabbedPane.getAccessibleContext().setAccessibleName("Remote Data");
        tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                boolean hasSelection = false;
                int index = tabbedPane.getSelectedIndex();
                if (index == 0) {
                    hasSelection = hasRemoteSelection();
                } else {
                    hasSelection = hasLocalSelection();
                }

                editEntryButton.setEnabled(hasSelection);
                editEntryItem.setEnabled(hasSelection);
                removeEntryButton.setEnabled(hasSelection);
                removeEntryItem.setEnabled(hasSelection);
                setLastTab(index);
            }
        });

        pack();
    }// </editor-fold>

    /**
     * I respond to events! Yyyyaaaaaaayyyyyy!!!!
     * 
     * @param e
     */
    private void remoteSelectionModelChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        ListSelectionModel selModel = (ListSelectionModel)e.getSource();
        List<RemoteAddeEntry> selectedEntries;
        if (selModel.isSelectionEmpty()) {
            selectedEntries = Collections.emptyList();
        } else {
            int min = selModel.getMinSelectionIndex();
            int max = selModel.getMaxSelectionIndex();
            RemoteAddeTableModel tableModel = ((RemoteAddeTableModel)remoteTable.getModel());
            selectedEntries = arrList();
            for (int i = min; i <= max; i++) {
                if (selModel.isSelectedIndex(i)) {
                    selectedEntries.add(tableModel.getEntryAtRow(i));
                }
            }
        }

        setSelectedRemoteEntries(selectedEntries);

        // the current "edit" dialog doesn't work so well with multiple 
        // servers/datasets, so only allow the user to edit entries one at a time.
        boolean singleSelection = selectedEntries.size() == 1;
        editEntryButton.setEnabled(singleSelection);
        editEntryItem.setEnabled(singleSelection);

        boolean hasSelection = !selectedEntries.isEmpty();
        removeEntryButton.setEnabled(hasSelection);
        removeEntryItem.setEnabled(hasSelection);
    }

    private void localSelectionModelChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        ListSelectionModel selModel = (ListSelectionModel)e.getSource();
        List<LocalAddeEntry> selectedEntries;
        if (selModel.isSelectionEmpty()) {
            selectedEntries = Collections.emptyList();
        } else {
            int min = selModel.getMinSelectionIndex();
            int max = selModel.getMaxSelectionIndex();
            LocalAddeTableModel tableModel = ((LocalAddeTableModel)localEntries.getModel());
            selectedEntries = arrList();
            for (int i = min; i <= max; i++) {
                if (selModel.isSelectedIndex(i)) {
                    selectedEntries.add(tableModel.getEntryAtRow(i));
                }
            }
        }

        setSelectedLocalEntries(selectedEntries);

        // the current "edit" dialog doesn't work so well with multiple 
        // servers/datasets, so only allow the user to edit entries one at a time.
        boolean singleSelection = (selectedEntries.size() == 1);
        editEntryButton.setEnabled(singleSelection);
        editEntryItem.setEnabled(singleSelection);

        boolean hasSelection = !selectedEntries.isEmpty();
        removeEntryButton.setEnabled(hasSelection);
        removeEntryItem.setEnabled(hasSelection);
    }

    private boolean hasRemoteSelection() {
        return !selectedRemoteEntries.isEmpty();
    }
    private boolean hasLocalSelection() {
        return !selectedLocalEntries.isEmpty();
    }
    
    private boolean hasSingleRemoteSelection() {
        return (selectedRemoteEntries.size() == 1);
    }

    private boolean hasSingleLocalSelection() {
        return (selectedLocalEntries.size() == 1);
    }

    private RemoteAddeEntry getSingleRemoteSelection() {
        RemoteAddeEntry entry = RemoteAddeEntry.INVALID_ENTRY;
        if (selectedRemoteEntries.size() == 1) {
            entry = selectedRemoteEntries.get(0);
        }
        return entry;
    }

    private LocalAddeEntry getSingleLocalSelection() {
        LocalAddeEntry entry = LocalAddeEntry.INVALID_ENTRY;
        if (selectedLocalEntries.size() == 1) {
            entry = selectedLocalEntries.get(0);
        }
        return entry;
    }

    private void setSelectedRemoteEntries(final Collection<RemoteAddeEntry> entries) {
        selectedRemoteEntries.clear();
        selectedRemoteEntries.addAll(entries);
    }

    private List<RemoteAddeEntry> getSelectedRemoteEntries() {
        if (selectedRemoteEntries.isEmpty()) {
            return Collections.emptyList();
        } else {
            return arrList(selectedRemoteEntries);
        }
    }

    private void setSelectedLocalEntries(final Collection<LocalAddeEntry> entries) {
        selectedLocalEntries.clear();
        selectedLocalEntries.addAll(entries);
    }

    private List<LocalAddeEntry> getSelectedLocalEntries() {
        if (selectedLocalEntries.isEmpty()) {
            return Collections.emptyList();
        } else {
            return arrList(selectedLocalEntries);
        }
    }

    private void newEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {
        showRemoteEditor();
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        logger.debug("formWindowClosed: evt={}", evt.toString());
        closeManager();
    }

    private void closeManager(java.awt.event.ActionEvent evt) {
        logger.debug("closeManager: evt={}", evt.toString());
        closeManager();
    }

    private void newLocalItemActionPerformed(java.awt.event.ActionEvent evt) {
        showLocalEditor();
    }

    private void newRemoteItemActionPerformed(java.awt.event.ActionEvent evt) {
        showRemoteEditor();
    }

    private void restartButtonActionPerformed(java.awt.event.ActionEvent evt) {
        restartLocalServer();
    }

    private void remoteHelpItemActionPerformed(java.awt.event.ActionEvent evt) {
        ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(REMOTE_HELP_TARGET);
    }

    private void localHelpItemActionPerformed(java.awt.event.ActionEvent evt) {
        ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(LOCAL_HELP_TARGET);
    }

    private void editEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (tabbedPane.getSelectedIndex() == 0) {
            showRemoteEditor(getSingleRemoteSelection());
        } else {
            showLocalEditor(getSingleLocalSelection());
        }
    }

    private void removeEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (tabbedPane.getSelectedIndex() == 0) {
            removeRemoteEntries(getSelectedRemoteEntries());
        } else {
            removeLocalEntries(getSelectedLocalEntries());
        }
    }

    private JPanel makeFileChooserAccessory() {
        JPanel accessory = new JPanel();
        accessory.setLayout(new BoxLayout(accessory, BoxLayout.PAGE_AXIS));
        importAccountBox = new javax.swing.JCheckBox("Use ADDE Accounting?");
        importAccountBox.setSelected(false);
        importAccountBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boolean selected = importAccountBox.isSelected();
                importUser.setEnabled(selected);
                importProject.setEnabled(selected);
            }
        });
        String clientProp = "JComponent.sizeVariant";
        String propVal = "mini";

        importUser = new javax.swing.JTextField();
        importUser.putClientProperty(clientProp, propVal);
        Prompt userPrompt = new Prompt(importUser, "Username");
        userPrompt.putClientProperty(clientProp, propVal);
        importUser.setEnabled(importAccountBox.isSelected());

        importProject = new javax.swing.JTextField();
        Prompt projPrompt = new Prompt(importProject, "Project Number");
        projPrompt.putClientProperty(clientProp, propVal);
        importProject.putClientProperty(clientProp, propVal);
        importProject.setEnabled(importAccountBox.isSelected());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(accessory);
        accessory.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importAccountBox)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(importProject, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importUser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(importAccountBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(importUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(importProject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(55, Short.MAX_VALUE))
        );
        return accessory;
    }

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser(getLastImportPath());
        fc.setAccessory(makeFileChooserAccessory());
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int ret = fc.showOpenDialog(TabbedAddeManager.this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            String path = f.getPath();
            importMctable(path);
            // don't worry about file validity; i'll just assume the user clicked
            // on the wrong entry by accident.
            setLastImportPath(f.getParent());
        }
    }

    /**
     * Returns the directory that contained the most recently imported MCTABLE.TXT.
     */
    private String getLastImportPath() {
        String path = "";
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv != null) {
            path = mcv.getObjectStore().get(LAST_IMPORTED, "");
        }
        return path;
    }

    /**
     * Saves the directory that contained the most recently imported MCTABLE.TXT.
     */
    private void setLastImportPath(final String path) {
        String okayPath = (path == null) ? "" : path;
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv != null) {
            mcv.getObjectStore().put(LAST_IMPORTED, okayPath);
            mcv.getObjectStore().saveIfNeeded();
        }
    }

    /**
     * Returns the index of the user's last server manager tab.
     */
    private int getLastTab() {
        int index = 0;
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv != null) {
            index = mcv.getObjectStore().get(LAST_TAB, 0);
        }
        return index;
    }

    /**
     * Saves the index of the last server manager tab the user was looking at.
     */
    private void setLastTab(final int index) {
        int okayIndex = ((index >= 0) && (index < 2)) ? index : 0;
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv == null) {
            return;
        }
        mcv.getObjectStore().put(LAST_TAB, okayIndex);
        mcv.getObjectStore().saveIfNeeded();
    }

    private static class RemoteAddeTableModel extends AbstractTableModel {

        /** Labels that appear as the column headers. */
        private final String[] columnNames = {
            "Dataset", "Accounting", "Types", "Source", "Validity"
        };

        /** Entries that currently populate the server manager. */
        private final List<RemoteAddeEntry> entries = arrList();

        /** {@link EntryStore} used to query and apply changes. */
        private final EntryStore entryStore;

        /**
         * 
         * 
         * @param entryStore
         */
        public RemoteAddeTableModel(final EntryStore entryStore) {
            notNull(entryStore, "Cannot query a null EntryStore");
            this.entryStore = entryStore;
            entries.addAll(entryStore.getRemoteEntries());
        }

        /**
         * Returns the {@link RemoteAddeEntry} at the given index.
         * 
         * @param row Index of the entry.
         * 
         * @return The {@code RemoteAddeEntry} at the index specified by {@code row}.
         */
        protected RemoteAddeEntry getEntryAtRow(final int row) {
            return entries.get(row);
        }

        /**
         * Returns the index of the given {@code entry}.
         * 
         * @see List#indexOf(Object)
         */
        protected int getRowForEntry(final RemoteAddeEntry entry) {
            return entries.indexOf(entry);
        }

        /**
         * Clears and re-adds all {@link RemoteAddeEntry}s within {@link #entries}. 
         */
        public void refreshEntries() {
            entries.clear();
            entries.addAll(entryStore.getRemoteEntries());
            this.fireTableDataChanged();
        }

        /**
         * Returns the length of {@link #columnNames}.
         * 
         * @return The number of columns.
         */
        @Override public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns the number of entries being managed.
         */
        @Override public int getRowCount() {
            return entries.size();
        }

        /**
         * Finds the value at the given coordinates.
         * 
         * @param row
         * @param column
         * 
         * @return
         * 
         * @throws IndexOutOfBoundsException
         */
        @Override public Object getValueAt(int row, int column) {
            RemoteAddeEntry entry = entries.get(row);
            if (entry == null) {
                throw new IndexOutOfBoundsException(); // questionable...
            }

            switch (column) {
                case 0: return entry.getEntryText();
                case 1: return formattedAccounting(entry);
                case 2: return entry.getEntryType();
                case 3: return entry.getEntrySource();
                case 4: return entry.getEntryValidity();
                default: throw new IndexOutOfBoundsException();
            }
        }

        private static String formattedAccounting(final RemoteAddeEntry entry) {
            AddeAccount acct = entry.getAccount();
            if (acct == RemoteAddeEntry.DEFAULT_ACCOUNT) {
                return "public dataset";
            }
            return acct.friendlyString();
        }

        /**
         * Returns the column name associated with {@code column}.
         * 
         * @return One of {@link #columnNames}.
         */
        @Override public String getColumnName(final int column) {
            return columnNames[column];
        }

        @Override public boolean isCellEditable(final int row, final int column) {
            switch (column) {
//                case 0: return true; // hiding alias stuff for now
                case 0: return false;
                case 1: return false;
                case 2: return false;
                case 3: return false;
                case 4: return false;
                case 5: return true;
                case 6: return false;
                default: throw new IndexOutOfBoundsException();
            }
        }
    }

    private static class LocalAddeTableModel extends AbstractTableModel {

        /** Labels that appear as the column headers. */
        private final String[] columnNames = {
            "Dataset (e.g. MYDATA)", "Image Type (e.g. JAN 07 GOES)", "Format", "Directory"
        };
//        private final String[] columnNames = {
//            "Alias", "Dataset (e.g. MYDATA)", "Image Type (e.g. JAN 07 GOES)", "Format", "Directory"
//        };

        /** Entries that currently populate the server manager. */
        private final List<LocalAddeEntry> entries = arrList();

        /** {@link EntryStore} used to query and apply changes. */
        private final EntryStore entryStore;

        public LocalAddeTableModel(final EntryStore entryStore) {
            notNull(entryStore, "Cannot query a null EntryStore");
            this.entryStore = entryStore;
            entries.addAll(entryStore.getLocalEntries());
        }

        /**
         * Returns the {@link LocalAddeEntry} at the given index.
         * 
         * @param row Index of the entry.
         * 
         * @return The {@code LocalAddeEntry} at the index specified by {@code row}.
         */
        protected LocalAddeEntry getEntryAtRow(final int row) {
            return entries.get(row);
        }

        protected int getRowForEntry(final LocalAddeEntry entry) {
            return entries.indexOf(entry);
        }

        protected List<LocalAddeEntry> getSelectedEntries(final int[] rows) {
            List<LocalAddeEntry> selected = arrList();
            int rowCount = entries.size();
            for (int i = 0; i < rows.length; i++) {
                int tmpIdx = rows[i];
                if ((tmpIdx >= 0) && (tmpIdx < rowCount)) {
                    selected.add(entries.get(tmpIdx));
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }
            return selected;
        }

        public void refreshEntries() {
            entries.clear();
            entries.addAll(entryStore.getLocalEntries());
            this.fireTableDataChanged();
        }

        /**
         * Returns the length of {@link #columnNames}.
         * 
         * @return The number of columns.
         */
        @Override public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns the number of entries being managed.
         */
        @Override public int getRowCount() {
            return entries.size();
        }

        /**
         * Finds the value at the given coordinates.
         * 
         * @param row
         * @param column
         * 
         * @return
         * 
         * @throws IndexOutOfBoundsException
         */
        @Override public Object getValueAt(int row, int column) {
            LocalAddeEntry entry = entries.get(row);
            if (entry == null)
                throw new IndexOutOfBoundsException(); // still questionable...
//            switch (column) {
//                case 0: return entry.getEntryAlias();
//                case 1: return entry.getGroup();
//                case 2: return entry.getName();
//                case 3: return entry.getFormat().getTooltip();
//                case 4: return entry.getMask();
//                default: throw new IndexOutOfBoundsException();
//            }
            switch (column) {
                case 0: return entry.getGroup();
                case 1: return entry.getName();
                case 2: return entry.getFormat();
                case 3: return entry.getMask();
                default: throw new IndexOutOfBoundsException();
            }
        }

        /**
         * Returns the column name associated with {@code column}.
         * 
         * @return One of {@link #columnNames}.
         */
        @Override public String getColumnName(final int column) {
            return columnNames[column];
        }
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TabbedAddeManager().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify
    private javax.swing.JPanel actionPanel;
    private javax.swing.JMenuItem closeItem;
    private javax.swing.JButton editEntryButton;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu editMenu;
    private javax.swing.JPopupMenu.Separator fileSeparator1;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JButton importButton;
    private javax.swing.JTable localEntries;
    private javax.swing.JScrollPane localEntriesScroller;
    private javax.swing.JMenuItem localHelpItem;
    private javax.swing.JPanel localTab;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton newEntryButton;
    private javax.swing.JMenuItem newLocalItem;
    private javax.swing.JMenuItem newRemoteItem;
    private javax.swing.JMenuItem remoteHelpItem;
    private javax.swing.JMenuItem editEntryItem;
    private javax.swing.JMenuItem removeEntryItem;
    private javax.swing.JScrollPane remoteScroller;
    private javax.swing.JPanel remoteTab;
    private javax.swing.JTable remoteTable;
    private javax.swing.JButton removeEntryButton;
    private javax.swing.JButton restartButton;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JCheckBox importAccountBox;
    private javax.swing.JTextField importUser;
    private javax.swing.JTextField importProject;
    // End of variables declaration
    
}
