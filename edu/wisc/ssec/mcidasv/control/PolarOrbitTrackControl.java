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

package edu.wisc.ssec.mcidasv.control;

import edu.wisc.ssec.mcidasv.data.GroundStations;
import edu.wisc.ssec.mcidasv.data.hydra.CurveDrawer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.GuiUtils.ColorSwatch;
import ucar.visad.display.TextDisplayable;

import visad.Data;
import visad.Gridded2DSet;
import visad.MathType;
import visad.RealTuple;
import visad.RealTupleType;
import visad.SampledSet;
import visad.Text;
import visad.TextControl;
import visad.TextControl.Justification;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.UnionSet;
import visad.VisADException;
import visad.georef.LatLonTuple;

/**
 * {@link ucar.unidata.idv.control.PolarOrbitTrackControl} with some McIDAS-V
 * specific extensions. Namely parameter sets and support for inverted 
 * parameter defaults.
 */
public class PolarOrbitTrackControl extends DisplayControlImpl {

    private static final Logger logger = LoggerFactory.getLogger(PolarOrbitTrackControl.class);

    /** The spacing used in the grid layout */
    protected static final int GRID_SPACING = 3;

    /** Used by derived classes when they do a GuiUtils.doLayout */
    protected static final Insets GRID_INSETS = new Insets(GRID_SPACING,
                                                    GRID_SPACING,
                                                    GRID_SPACING,
                                                    GRID_SPACING);
    private double latitude;
    private double longitude;
    private double altitude;
    private JPanel fontSizePanel;
    private JPanel colorPanel;
    private JPanel locationPanel;
    private JPanel latLonAltPanel;

    private List stations;
    private List lats;
    private List lons;
    private List alts;

    private JComboBox locationComboBox;

    /** Input for lat/lon center point */
    protected LatLonWidget latLonWidget = new LatLonWidget();

    private JTextField latFld;
    private JTextField lonFld;
    private JTextField altitudeFld = new JTextField(" ", 5);

//    private ChangeListener sizeListener;
    private ActionListener fontSizeChange;
    private FocusListener fontSizeFocusChange;

    /** Font size control */
    private static final int SLIDER_MAX = 10;
    private static final int SLIDER_MIN = 1;
    private static final int SLIDER_WIDTH = 150;
    private static final int SLIDER_HEIGHT = 16;

    private JSlider fontSizeSlider;
    private JTextField fontSizeFld = new JTextField();

    private CurveDrawer trackDsp;
    private List <TextDisplayable> timeLabels = new ArrayList();
    private static final TupleType TUPTYPE = makeTupleType();

    private int fontSize;
    private int defaultSize = 3;
    private ColorSwatch colorSwatch;
    private Color color;
    private Color defaultColor = Color.GREEN;

    public PolarOrbitTrackControl() {
        super();
        logger.trace("created new tlecontrol={}", Integer.toHexString(hashCode()));
    }

    @Override public boolean init(DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        boolean result = super.init((DataChoice)this.getDataChoices().get(0));

        Data data = getData(getDataInstance());
        createTrackDisplay(data);
        return result;
    }

    private void createTrackDisplay(Data data) {
        try {
            this.fontSize = getFontSize();
            this.color = getColor();
            List<String> dts = new ArrayList();
            if (data instanceof Tuple) {
                Data[] dataArr = ((Tuple)data).getComponents();
                int npts = dataArr.length;
                float[][] latlon = new float[2][npts];
                float fSize = this.fontSize/10.f;
                for (int i=0; i<npts; i++) {
                    Tuple t = (Tuple)dataArr[i];
                    Data[] tupleComps = t.getComponents();
                    String str = ((Text)tupleComps[0]).getValue();
                    dts.add(str);
                    int indx = str.indexOf(" ") + 1;
                    String subStr = "- " + str.substring(indx, indx+5);

                    TextDisplayable time = new TextDisplayable(TextType.Generic);
                    time.setJustification(TextControl.Justification.LEFT);
                    time.setVerticalJustification(TextControl.Justification.CENTER);
                    time.setLineWidth(2f);
                    time.setTextSize(fSize);
                    time.setColor(this.color);
                    
                    addDisplayable(time, FLAG_COLORTABLE);

                    LatLonTuple llt = (LatLonTuple)tupleComps[1];
                    double dlat = llt.getLatitude().getValue();
                    double dlon = llt.getLongitude().getValue();
                    RealTuple lonLat =
                        new RealTuple(RealTupleType.SpatialEarth2DTuple,
                            new double[] { dlon, dlat });
                    Tuple tup = new Tuple(TUPTYPE,
                        new Data[] { lonLat, new Text(subStr)});
                    time.setData(tup);
                    this.timeLabels.add(time);
                    float lat = (float)dlat;
                    float lon = (float)dlon;
                    //System.out.println("    Time=" + subStr + " Lat=" + lat + " Lon=" + lon);
                    latlon[0][i] = lat;
                    latlon[1][i] = lon;
                }
                Gridded2DSet track = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                           latlon, npts);
                SampledSet[] set = new SampledSet[1];
                set[0] = track;
                UnionSet uset = new UnionSet(set);
                this.trackDsp = new CurveDrawer(uset);
                this.trackDsp.setColor(this.color);
                this.trackDsp.setData(uset);
                addDisplayable(this.trackDsp, FLAG_COLORTABLE);
            }
        } catch (Exception e) {
            System.out.println("getData e=" + e);
        }
        return;
    }

    private static TupleType makeTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] {RealTupleType.SpatialEarth2DTuple,
                                              TextType.Generic});
        } catch (Exception e) {
            System.out.println("\nPolarOrbitTrackControl.makeTupleType e=" + e);
        }
        return t;
    }

    public JComponent makeColorBox(Color swatchColor) {
        GuiUtils.ColorSwatch swatch = new GuiUtils.ColorSwatch(swatchColor,
                                               "Color") {
            public void userSelectedNewColor(Color c) {
                try {
                    getIdv().showWaitCursor();
                    setColor(c);
                    setBackground(c);
                    getIdv().showNormalCursor();
                } catch (Exception e) {
                    System.out.println("\nsetColor e=" + e);
                    setColor(defaultColor);
                }
            }
        };
        return swatch;
    }

    /**
     * Called by doMakeWindow in DisplayControlImpl, which then calls its
     * doMakeMainButtonPanel(), which makes more buttons.
     *
     * @return container of contents
     */
    public Container doMakeContents() {
/*
        this.sizeListener =
            new javax.swing.event.ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                int val = getSizeValue(fontSizeSlider);
                setFontSize(val);
            }
        };
*/
        this.fontSizeChange =new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String str = fontSizeFld.getText();
                int size = new Integer(str).intValue();
                moveFontSizeSlider(size);
                setDisplayableTextSize(size);
            }
        };
        this.fontSizeFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {
            }
            public void focusLost(FocusEvent fe) {
                String str = fontSizeFld.getText();
                int size = new Integer(str).intValue();
                moveFontSizeSlider(size);
                setDisplayableTextSize(size);
            }
        };

        this.fontSizeSlider = GuiUtils.makeSlider(SLIDER_MIN, SLIDER_MAX, defaultSize,
                                     this, "sliderChanged", true);
        this.fontSizeSlider.setPreferredSize(new Dimension(SLIDER_WIDTH,SLIDER_HEIGHT));
        this.fontSizeSlider.setMajorTickSpacing(1);
        this.fontSizeSlider.setSnapToTicks(true);
        int size = getSizeValue(this.fontSizeSlider);
        setFontSize(size);
        this.fontSizeFld = new JTextField(Integer.toString(size),3);
        this.fontSizeFld.addFocusListener(this.fontSizeFocusChange);
        this.fontSizeFld.addActionListener(this.fontSizeChange);
        this.fontSizePanel = GuiUtils.doLayout( new Component[] {
                 new JLabel("FontSize: "),
                 this.fontSizeFld,
                 new JLabel(" "),
                 this.fontSizeSlider }, 4,
                 GuiUtils.WT_N, GuiUtils.WT_N);

        Color swatchColor = getColor();
        colorSwatch = (GuiUtils.ColorSwatch)makeColorBox(swatchColor);
        colorPanel = GuiUtils.doLayout(new Component[] {
                           new JLabel("Set Color: "),
                           colorSwatch }, 2,
                           GuiUtils.WT_N, GuiUtils.WT_N);
        GroundStations gs = new GroundStations();
        int gsCount = gs.getGroundStationCount();
        String[] stats = new String[gsCount];
        stations = gs.getStations();
        for (int i=0; i<gsCount; i++) {
            stats[i] = (String)stations.get(i);
        }
        lats = gs.getLatitudes();
        lons = gs.getLongitudes();
        alts = gs.getAltitudes();

        locationComboBox = new JComboBox(stats);
        locationComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int indx = locationComboBox.getSelectedIndex();
                String str = (String)(lats.get(indx));
                Double d = new Double(str);
                double dVal = d.doubleValue();
                latLonWidget.setLat(dVal);
                str = (String)(lons.get(indx));
                d = new Double(str);
                dVal = d.doubleValue() * -1;
                latLonWidget.setLon(dVal);
                str = (String)(alts.get(indx));
                altitudeFld.setText(str);
            }
        });

        String str = (String)(lats.get(0));
        Double d = new Double(str);
        double dVal = d.doubleValue();
        latLonWidget.setLat(dVal);
        str = (String)(lons.get(0));
        d = new Double(str);
        dVal = d.doubleValue() * -1;
        latLonWidget.setLon(dVal);
        str = (String)(alts.get(0));
        altitudeFld = new JTextField(str, 5);
        latFld = latLonWidget.getLatField();
        lonFld = latLonWidget.getLonField();
        FocusListener latLonFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {
                latFld.setCaretPosition(latFld.getText().length());
                lonFld.setCaretPosition(lonFld.getText().length());
            }
            public void focusLost(FocusEvent fe) {
                setLatitude();
                setLongitude();
                setAltitude();
            }
        };
        locationPanel = GuiUtils.doLayout(new Component[] {
                           new JLabel("Ground Station: "),
                           locationComboBox }, 2,
                           GuiUtils.WT_N, GuiUtils.WT_N);
        latLonAltPanel = GuiUtils.doLayout(new Component[] {
                           latLonWidget,
                           new JLabel(" Altitude: "),
                           altitudeFld }, 3,
                           GuiUtils.WT_N, GuiUtils.WT_N);
        Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
        String  dfltLblSpacing  = " ";
        List allComps = new ArrayList();

        allComps.add(fontSizePanel);
        allComps.add(colorPanel);
        allComps.add(new JLabel(" "));
        allComps.add(locationPanel);
        allComps.add(latLonAltPanel);
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel dateTimePanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_NY,
                               GuiUtils.WT_N);
        return GuiUtils.top(dateTimePanel);
    }

    private int getSizeValue(JSlider slider) {
        int value = slider.getValue();
        if (value < SLIDER_MIN) {
            value = SLIDER_MIN;
        } else if (value > SLIDER_MAX) {
            value = SLIDER_MAX;
        }
        return value;
    }

    public int getFontSize() {
        if (this.fontSize < 1) this.fontSize = defaultSize;
        return this.fontSize;
    }

    public void setFontSizeTextField(int size) {
        size = setFontSize(size);
        try {
            if (this.fontSizeFld != null) {
                this.fontSizeFld.setText(new Integer(size).toString());
            }
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.setFontSizeTextField e=" + e);
        }
    }

    private void moveFontSizeSlider(int size) {
        size = setFontSize(size);
        try {
            if (this.fontSizeSlider != null) {
                this.fontSizeSlider.setValue(size);
            }
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.moveFontSizeSlider e=" + e);
        }
    }

    private void setDisplayableTextSize(int size) {
        //System.out.println("setDisplayableTextSize: size=" + size);
        size = setFontSize(size);
        try {
            float fSize = (float)size/10.0f;
            int num = this.timeLabels.size();
            TextDisplayable td = null;
            for (int i=0; i<num; i++) {
                td = (TextDisplayable)(this.timeLabels.get(i));
                td.setTextSize(fSize);
            }
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.setDisplayableTextSize e=" + e);
        }
    }

    public int setFontSize(int size) {
        if (size < 1) size = defaultSize;
        this.fontSize = size;
        return this.fontSize;
    }

    public Color getColor() {
        if (this.color == null) this.color = defaultColor;
        return this.color;
    }

    public void setColor(Color color) {
        if (this.color == null) this.color = defaultColor;
        try {
            this.trackDsp.setColor(color);
            int num = this.timeLabels.size();
            for (int i=0; i<num; i++) {
                ((TextDisplayable)(this.timeLabels.get(i))).setColor(color);
            }
            this.color = color;
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.setColor e=" + e);
        }
    }

    public void setLatitude() {
        this.latitude = latLonWidget.getLat();
    }

    public void setLongitude() {
        this.longitude = latLonWidget.getLon();
    }

    public void setAltitude() {
        String str = altitudeFld.getText();
        Double d = new Double(str);
        this.altitude = d.doubleValue();
    }

    public void sliderChanged(int sliderValue) {
        setFontSizeTextField(sliderValue);
        setDisplayableTextSize(sliderValue);
    }
}