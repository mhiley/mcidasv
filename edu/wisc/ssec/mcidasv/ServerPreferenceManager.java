package edu.wisc.ssec.mcidasv;

import edu.wisc.ssec.mcidas.adde.AddeServerInfo;

import edu.wisc.ssec.mcidasv.chooser.ServerInfo;
import edu.wisc.ssec.mcidasv.chooser.ServerDescriptor;

import java.awt.*;
import java.awt.event.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.idv.chooser.adde.AddeServer;

import ucar.unidata.ui.CheckboxCategoryPanel;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


public class ServerPreferenceManager extends IdvManager implements ActionListener {


    /** Should we show all of the display control descriptors */
    protected boolean showAllServers = true;

    /** A mapping that holds the servers that should be shown */
    protected Hashtable serversToShow = null;

    /** A mapping that holds the choosers that should be shown */
    protected Hashtable typesToShow = null;

    /** mapping between types and servers */
    private Hashtable cbxToServerMap;

    /** add server dialog */
    private JFrame addWindow;


    /** Shows the status */
    private JLabel statusLabel;

    /** _more_          */
    private JComponent statusComp;

    private PreferenceManager serversManager = null;
    private JPanel serversPanel = null;

    private final JButton deleteServer = new JButton("Delete");
    private ServerInfo si;

    private static String user;
    private static String proj;

    private Hashtable catMap = new Hashtable();

    private String[] allTypes = {"image", "point", "grid", "text", "nav"};

    private List allServers = new ArrayList();
    private List servImage = new ArrayList();
    private List servPoint = new ArrayList();
    private List servGrid = new ArrayList();
    private List servText = new ArrayList();
    private List servNav = new ArrayList();

    /** Install data type cbxs */
    private JCheckBox imageTypeCbx;
    private JCheckBox pointTypeCbx;
    private JCheckBox gridTypeCbx;
    private JCheckBox textTypeCbx;
    private JCheckBox navTypeCbx;

    private String lastCat;
    private JPanel lastPan;
    private JCheckBox lastBox;

    /** Install server and group name flds */
    private JTextField serverFld;
    private JTextField groupFld;

    /** tags */
    public static final String TAG_TYPE = "type";
    public static final String TAG_SERVER = "server";
    public static final String TAG_SERVERS = "servers";
    public static final String TAG_USERID = "userID";
    public static final String TAG_GROUP = "group";

    /** attributes */
    public static final String ATTR_ACTIVE = "active";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_NAMES = "names";
    public static final String ATTR_GROUP = "group";
    public static final String ATTR_USER = "user";
    public static final String ATTR_PROJ = "proj";
    public static final String ATTR_TYPE = "type";

    private final XmlResourceCollection serversXRC = getServers();

    /** Action command used for the Cancel button */
    private static String CMD_VERIFY = "Verify";

    /**
     * Create the dialog with the given idv
     *
     * @param idv The IDV
     *
     */
    public ServerPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
    }


    public void addServerPreferences(IdvPreferenceManager ipm) {
        getServerPreferences();
        ipm.add("ADDE Servers",
                 "What servers should be shown in choosers?",
                 serversManager, serversPanel, cbxToServerMap);
    }

    protected JComponent getStatusComponent() {
        if (statusComp == null) {
            JLabel statusLabel = getStatusLabel();
            statusComp = GuiUtils.inset(statusLabel, 2);
            statusComp.setBackground(new Color(255, 255, 204));
        }
        return statusComp;
    }

    /**
     * Create (if needed) and return the JLabel that shows the status messages.
     *
     * @return The status label
     */
    protected JLabel getStatusLabel() {
        if (statusLabel == null) {
            statusLabel = new JLabel();
        }
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(255, 255, 204));
        return statusLabel;
    }

    public void setStatus(String msg) {
        getStatusLabel().setText(msg);
        serversPanel.paintImmediately(0,0,serversPanel.getWidth(),
                                        serversPanel.getHeight());
    }


    /**
     * Add in the user preference tab for the servers to show.
     */
    protected void getServerPreferences() {
        cbxToServerMap = new Hashtable();
        List servList = new ArrayList();
        si = new ServerInfo(getIdv(), serversXRC);
        final List catPanels          = new ArrayList();
        List types = si.getServerTypes();
        String typeString;

        deleteServer.setEnabled(false);
        deleteServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                deleteServers();
                deleteServer.setEnabled(false);
            }
        });

        for (int i=0; i<types.size(); i++) {
            typeString = (String)types.get(i);
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(typeString);
            if (catPanel == null) {
                catPanel = new CheckboxCategoryPanel(typeString, false);
                catPanels.add(catPanel);
                catMap.put(typeString, catPanel);
                servList.add(catPanel.getTopPanel());
                servList.add(catPanel);
            }
            List servers = si.getServers(typeString, true, true);
            if (servers.size() > 0) {
                for (int j=0; j<servers.size(); j++) {
                    final ServerDescriptor sd = (ServerDescriptor)servers.get(j);
                    allServers.add(sd);
                    final JCheckBox cbx = new JCheckBox(sd.toString(), sd.getIsActive());
                    final String str = typeString;
                    final int indx = j;
                    final JPanel pan = GuiUtils.inset(cbx, new Insets(0, 20, 0, 0));
                    final CheckboxCategoryPanel catpan = catPanel;
                    cbx.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            lastBox = cbx;
                            lastCat = str;
                            lastPan = pan;
                            sd.setIsActive(!sd.getIsActive());
                            deleteServer.setEnabled(true);
                        }
                    });
                    cbxToServerMap.put(cbx, sd);
                    catPanel.add(pan);
                }
            }
        }

        for (int i = 0; i < catPanels.size(); i++) {
            ((CheckboxCategoryPanel) catPanels.get(i)).checkVisCbx();
        }

        List comps = new ArrayList();
        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Hashtable table = cbxToServerMap;
                for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox cbx = (JCheckBox)keys.nextElement();
                    ServerDescriptor sd = (ServerDescriptor)table.get(cbx);
                    sd.setIsActive(true);
                    cbx.setSelected(true);
                }
                for (int i = 0; i < catPanels.size(); i++) {
                    CheckboxCategoryPanel cPanel = (CheckboxCategoryPanel) catPanels.get(i);
                    cPanel.toggleAll(true);
                }
            }
        });
        comps.add(allOn);
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Hashtable table = cbxToServerMap;
                for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox cbx = (JCheckBox)keys.nextElement();
                    ServerDescriptor sd = (ServerDescriptor)table.get(cbx);
                    sd.setIsActive(false);
                    cbx.setSelected(false);
                }
                for (int i = 0; i < catPanels.size(); i++) {
                    CheckboxCategoryPanel cPanel = (CheckboxCategoryPanel) catPanels.get(i);
                    cPanel.toggleAll(false);
                }
            }
        });
        comps.add(allOff);
        comps.add(new JLabel(" "));
        final JButton addServer = new JButton("Add");
        addServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addServers();
            }
        });
        comps.add(addServer);
        comps.add(deleteServer);

        final JPanel servPanel = GuiUtils.doLayout(new JPanel(), 
             GuiUtils.getComponentArray(servList), 1, GuiUtils.WT_Y, GuiUtils.WT_Y);
        JScrollPane  servScroller = new JScrollPane(servPanel);
        servScroller.getVerticalScrollBar().setUnitIncrement(10);
        servScroller.setPreferredSize(new Dimension(300, 300));

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals("import")) {
                    showWaitCursor();
                    getServersFromMctable();
                    showNormalCursor();
                } else if (cmd.equals("export")) {
                    exportServersToPlugin();
                }
            }
        };
        String[] labels = {"Import from McIDAS-X", "Export to Plugin"};
        String[] cmds = {"import", "export"};
        JComponent exportImportServers =
            GuiUtils.right(GuiUtils.makeButtons(listener, labels, cmds));
        final JPanel exportImportPanel = (JPanel)exportImportServers;
        Component[] cmps = exportImportPanel.getComponents();
        JComponent servComp = GuiUtils.centerBottom(servScroller, exportImportServers);

        JPanel bottomPanel =
            GuiUtils.leftCenter(
                GuiUtils.inset(
                    GuiUtils.top(GuiUtils.vbox(comps)),
                    4), new Msg.SkipPanel(
                        GuiUtils.hgrid(
                            Misc.newList(servComp, GuiUtils.filler()), 0)));

        serversPanel =
            GuiUtils.inset(GuiUtils.topCenter( GuiUtils.vbox(new JLabel(" "),
                GuiUtils.hbox(GuiUtils.rLabel("Status: "),getStatusComponent()),
                new JLabel(" "), new JLabel(" ")),
                bottomPanel), 6);
        GuiUtils.enableTree(servPanel, true);

        allOn.setEnabled( true);
        allOff.setEnabled( true);

        serversManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                updateXml();
            }
        };
    }


    /**
     * Add servers
     */
    private void addServers() {
        if (addWindow == null) {
            showAddDialog();
            return;
        }
        addWindow.setVisible(true);
        GuiUtils.toFront(addWindow);
    }


    /**
     * Delete server
     */
    private void deleteServers() {
        if (lastCat != null) {
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(lastCat);
            cbxToServerMap.remove(lastBox);
            if (catPanel.getComponentCount() == 1)
                catPanel.setVisible(false);
            catPanel.remove(lastPan);
            catPanel.validate();
        }
    }


    /**
     * showAddDialog
     */
    private void showAddDialog() {
        if (addWindow == null) {
            List comps = new ArrayList();
            comps.add(imageTypeCbx =
                new JCheckBox("Image", false));
            comps.add(pointTypeCbx =
                new JCheckBox("Point", false));
            comps.add(gridTypeCbx =
                new JCheckBox("Grid", false));
            comps.add(textTypeCbx =
                new JCheckBox("Text", false));
            comps.add(navTypeCbx =
                new JCheckBox("Navigation", false));

            JPanel dataTypes = GuiUtils.inset(GuiUtils.hbox(comps, 5),20);

            addWindow = GuiUtils.createFrame("Add Server");

            serverFld = new JTextField("", 30);
            groupFld = new JTextField("", 30);

            List textComps = new ArrayList();
            textComps.add(new JLabel(" "));
            textComps.add(GuiUtils.hbox(new JLabel("Server: "), serverFld));
            textComps.add(new JLabel(" "));
            textComps.add(GuiUtils.hbox(new JLabel("Group(s): "), groupFld));
            textComps.add(new JLabel(" "));
            JComponent nameComp = GuiUtils.center(GuiUtils.inset(
                                     GuiUtils.vbox(textComps),20));

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String cmd = event.getActionCommand();
                    if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                        addWindow.setVisible(false);
                        addWindow = null;
                    } else {
                        String newServer = serverFld.getText().trim();
                        String newGroup = groupFld.getText().trim();
                        List typeList = new ArrayList();
                        if (imageTypeCbx.isSelected()) typeList.add("image");
                        if (pointTypeCbx.isSelected()) typeList.add("point");
                        if (gridTypeCbx.isSelected()) typeList.add("grid");
                        if (textTypeCbx.isSelected()) typeList.add("text");
                        if (navTypeCbx.isSelected()) typeList.add("nav");
                        if (cmd.equals(CMD_VERIFY)) {
                            imageTypeCbx.setSelected(checkServer(newServer, "image", newGroup));
                            pointTypeCbx.setSelected(checkServer(newServer, "point", newGroup));
                            gridTypeCbx.setSelected(checkServer(newServer, "grid", newGroup));
                            textTypeCbx.setSelected(checkServer(newServer, "text", newGroup));
                            navTypeCbx.setSelected(checkServer(newServer, "nav", newGroup));
                        } else {
                            addNewServer(newServer, newGroup, typeList);
                            closeAddServer();
                        }
                    }
                }
            };


            JPanel bottom =
                //GuiUtils.inset(GuiUtils.makeApplyCancelButtons(listener),5);
                GuiUtils.inset(makeVerifyApplyCancelButtons(listener),5);
            JComponent contents = GuiUtils.topCenterBottom(nameComp, dataTypes, bottom);
            addWindow.getContentPane().add(contents);
            addWindow.pack();
            addWindow.setLocation(200, 200);
        }
        addWindow.setVisible(true);
        GuiUtils.toFront(addWindow);
    }

    /**
     * Utility to make verify/apply/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return The button panel
     */
    public static JPanel makeVerifyApplyCancelButtons(ActionListener l) {
        return GuiUtils.makeButtons(l, new String[] { "Verify", "Apply", "Cancel" },
                           new String[] { CMD_VERIFY,
                                          GuiUtils.CMD_APPLY,
                                          GuiUtils.CMD_CANCEL });
    }

    /**
     * Close the add dialog
     */
    public void closeAddServer() {
        if (addWindow != null) {
            addWindow.setVisible(false);
        }
    }
     
    /**
     * Import the servers and groups from MCTABLE
     */
    public void getServersFromMctable() {
        setStatus("Locate MCTABLE.TXT");
        List addeServers = new ArrayList();
        JFileChooser chooser = new JFileChooser();
        PatternFileFilter ff = new PatternFileFilter("MCTABLE.TXT", "McIDAS-X ADDE Routing Table");
        chooser.setFileFilter(ff);
        chooser.showOpenDialog(null);
        File file = chooser.getSelectedFile();
        if (file == null) return;
        setStatus("Checking user and project number...");
        setUserProj();

        StringTokenizer tok;
        String next;
        try {
            setStatus("Reading MCTABLE.TXT...");
            InputStream is = IOUtil.getInputStream(file.toString());
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));
            String lineOut = reader.readLine();
            String lineType;
            String serv;
            StringTokenizer tokTwo;
            AddeServer as = new AddeServer();
            CharSequence dot = (CharSequence)".";
            while (lineOut != null) {
                tok = new StringTokenizer(lineOut, "_");
                lineType = tok.nextToken();
                next = tok.nextToken();
                if (lineType.equals("HOST")) {
                    tokTwo = new StringTokenizer(next, "=");
                    String server = tokTwo.nextToken();
                    if (!server.contains(dot)) {
                        next = tokTwo.nextToken();
                        for (int i=0; i<addeServers.size(); i++) {
                            as = (AddeServer)addeServers.get(i);
                            serv = (String)as.getName();
                            if (serv.equals(server)) {
                                as.setName(next);
                                addeServers.set(i,as);
                            }
                        }
                    }
                } else if (lineType.equals("ADDE")) {
                    if (next.equals("ROUTE")) {
                        next = tok.nextToken();
                        tokTwo = new StringTokenizer(next,"=");
                        next = tokTwo.nextToken();
                        serv = tokTwo.nextToken();
                        if (!serv.equals("LOCAL-DATA")) {
                            for (int typeIdx=0; typeIdx<allTypes.length; typeIdx++) {
                                String typ = allTypes[typeIdx];
                                setStatus(serv + "/" + next + "   Checking for " + typ);
                                if (checkServer(serv, typ, next)) {
                                    as = new AddeServer(serv);
                                    AddeServer.Group g = new AddeServer.Group(typ, next, "");
                                    as.addGroup(g);
                                    addeServers.add(as);
                                    List typeList = new ArrayList();
                                    typeList.add(typ);
                                    addNewServer(serv, next, typeList);
                                }
                            }
                        }
                    }
                }
                lineOut = reader.readLine();
            } 
        } catch (Exception e) {
            System.out.println("getServersFromMctable e=" + e);
            return;
        }

        List serversFinal = AddeServer.coalesce(addeServers);

        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
        Element serverRoot = serverCollection.getWritableRoot("<tabs></tabs>");
        Document serverDocument = serverCollection.getWritableDocument("<tabs></tabs>");
        try {
            Element serversEle = AddeServer.toXml(serversFinal, false);
            serversEle.setAttribute(ATTR_USER, user);
            serversEle.setAttribute(ATTR_PROJ, proj);
            serverCollection.setWritableDocument(serverDocument, serversEle);
            serverCollection.writeWritable();
        } catch (Exception e) {
            System.out.println("AddeServer.toXml e=" + e);
        }
        si = null;
        setStatus("Done");
        return;
    }

    private void setUserProj() {
        if ((!user.equals("")) & (!proj.equals(""))) return;
        String pus = JOptionPane.showInputDialog(
            "User ID and project number required \nPlease enter them here (eg., JACK 1234)");
        if (pus != null) {
            StringTokenizer stp = new StringTokenizer(pus," ");
            if (stp.countTokens() == 2) {
                user = stp.nextToken();
                proj = stp.nextToken();
            }
        }
        if (si == null) {
            si = new ServerInfo(getIdv(), serversXRC);
        }
    }

    private boolean checkServer(String server, String type, String group) {
        String[] servers = { server };
        AddeServerInfo asi = new AddeServerInfo(servers);
        int stat = asi.setSelectedServer(server , type.toUpperCase());
        if (stat == -1) {
            setUserProj();
            asi.setUserIDandProjString("user=" + user + "&proj=" + proj);
            stat = asi.setSelectedServer(server , type.toUpperCase());
        }
        if (stat < 0) return false;
        asi.setSelectedGroup(group);
        String[] datasets = asi.getDatasetList();
        int len =0;
        try {
            len = datasets.length;
        } catch (Exception e) {};
        if (len < 1) return false;
        return true;
    }

    private void addNewServer(String newServer, String grp, List type) {
        showWaitCursor();
        StringTokenizer tok = new StringTokenizer(grp, ",");
        List newGroups = new ArrayList();
        while (tok.hasMoreTokens()) {
            newGroups.add(tok.nextToken().trim());
        }
        String typeString = "";
        if (type != null) {
            for (int i=0; i<type.size(); i++) {
                typeString =(String)type.get(i);
                for (int j=0; j<newGroups.size(); j++) {
                    ServerDescriptor sd = new ServerDescriptor(typeString,
                        newServer, (String)newGroups.get(j), "true");
                    final JCheckBox cbx = new JCheckBox(sd.toString(), sd.getIsActive());
                    cbxToServerMap.put(cbx, sd);
                    CheckboxCategoryPanel catPanel =
                        (CheckboxCategoryPanel) catMap.get(typeString);
                    final JPanel pan = GuiUtils.inset(cbx, new Insets(0, 20, 0, 0));
                    catPanel.add(pan);
                    final String str = typeString;
                    cbx.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            lastCat = str;
                            lastBox = cbx;
                            lastPan = pan;
                            deleteServer.setEnabled(true);
                        }
                    });
                    catPanel.validate();
                }
            }
        }
        showNormalCursor();
    }


    /**
     * Export the selected servers to the plugin manager
     */
    public void exportServersToPlugin() {
/*
        Hashtable    selected           = new Hashtable();
        Hashtable    table              = cbxToCdMap;
        List         controlDescriptors = getIdv().getAllControlDescriptors();
        StringBuffer sb                 =
            new StringBuffer(XmlUtil.XML_HEADER);
        sb.append("<" + ControlDescriptor.TAG_CONTROLS + ">\n");
        for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
            JCheckBox cbx = (JCheckBox) keys.nextElement();
            if ( !cbx.isSelected()) {
                continue;
            }
            ControlDescriptor cd = (ControlDescriptor) table.get(cbx);
            cd.getDescriptorXml(sb);
        }

        sb.append("</" + ControlDescriptor.TAG_CONTROLS + ">\n");
        getIdv().getPluginManager().addText(sb.toString(), "controls.xml");
*/
    }

    /**
     * Update servers.xml
     */
    private void updateXml() {
        List servers = new ArrayList();
        Hashtable table = cbxToServerMap;
        allServers.clear();
        for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
            JCheckBox cbx = (JCheckBox)keys.nextElement();
            ServerDescriptor sd = (ServerDescriptor)table.get(cbx);
            sd.setIsActive(true);
            if (!cbx.isSelected()) {
                sd.setIsActive(false);
            }
            allServers.add(sd);
            AddeServer as = new AddeServer(sd.getServerName());
            AddeServer.Group g = new AddeServer.Group(sd.getDataType(), 
                                         sd.getGroupName(), "");
            g.setActive(sd.getIsActive());
            as.addGroup(g);
            servers.add(as);
        }
        List serversFinal = AddeServer.coalesce(servers);

        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
        Element serverRoot = serverCollection.getWritableRoot("<tabs></tabs>");
        Document serverDocument = serverCollection.getWritableDocument("<tabs></tabs>");
        try {
            Element serversEle = AddeServer.toXml(serversFinal, false);
            serversEle.setAttribute(ATTR_USER, user);
            serversEle.setAttribute(ATTR_PROJ, proj);
            serverCollection.setWritableDocument(serverDocument, serversEle);
            serverCollection.writeWritable();
        } catch (Exception e) {
            System.out.println("updateXml AddeServer.toXml e=" + e);
        }
    }
                
    /**
     * Get the xml resource collection that defines the servers xml
     *
     * @return server resources
     */
    protected XmlResourceCollection getServers() {
        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
        si = new ServerInfo(getIdv(), serverCollection);
        user = si.getUser();
        proj = si.getProj();
        return serverCollection;
    }
}
