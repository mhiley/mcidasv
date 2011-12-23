/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2011
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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;

/**
 * Utility class to support Joint Polar Satellite System (JPSS) functionality.
 * Some of this is temporary hashmaps, needed because the currently available
 * NPP data does not fully, correctly implement the specs yet as defined in
 * the NPP Common Data Format Control Book.  See:
 * http://jointmission.gsfc.nasa.gov/project/science-documents.html
 * 
 * @author tommyj
 *
 */

public abstract class JPSSUtilities {
   
	public static final String JPSS_FIELD_SEPARATOR = "_";
	
	// This regular expression matches an NPP Data Product as defined by the 
	// spec in CDFCB-X Volume 1, Page 21
	public static final String NPP_REGEX =
    		// Product Id, Multiple (ex: VSSTO-GATMO-VSLTO)
    		"(\\w\\w\\w\\w\\w-)*" + 
    		// Product Id, Single (ex: VSSTO)
    		"\\w\\w\\w\\w\\w" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Spacecraft Id (ex: npp)
    		"\\w\\w\\w" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Date (ex: dYYYYMMDD)
    		"d20[0-3]\\d[0-1]\\d[0-3]\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Time (ex: tHHMMSSS)
    		"t[0-2]\\d[0-5]\\d[0-6]\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Stop Time (ex: eHHMMSSS)
    		"e[0-2]\\d[0-5]\\d[0-6]\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Orbit Number (ex: b00015)
    		"b\\d\\d\\d\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Creation Date (ex: cYYYYMMDDHHMMSSSSSSSS)
    		"c20[0-3]\\d[0-1]\\d[0-3]\\d[0-2]\\d[0-5]\\d[0-6]\\d\\d\\d\\d\\d\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Origin (ex: navo)
    		"\\w\\w\\w\\w" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Domain (ex: ops)
    		"\\w\\w\\w" + 
    		// HDF5 suffix
    		".h5";
	
	public static float[] ATMSChannelCenterFrequencies = {
		23.8f,
		31.4f,
		50.3f,
		51.76f,
		52.8f,
		53.596f,
		54.40f,
		54.94f,
		55.50f,
		57.2903f,
		57.2903f,
		57.2903f,
		57.2903f,
		57.2903f,
		57.2903f,
		88.20f,
		165.5f,
		183.31f,
		183.31f,
		183.31f,
		183.31f,
		183.31f
	};
	
	// the list of valid geolocation product ids
	public static String[] geoProductIDs = {
    	"GATMO",
    	"GCRSO",
    	"GAERO",
    	"GCLDO",
    	"GDNBO",
    	"GNCCO",
    	"GIGTO",
    	"GIMGO",
    	"GITCO",
    	"GMGTO",
    	"GMODO",
    	"GMTCO",
    	"GNHFO",
    	"GOTCO",
    	"GOSCO",
    	"GONPO",
    	"GONCO",
    	"GCRIO",
    	"GATRO",
    	"IVMIM",
    	"VMUGE"
	};
	
    private static HashMap<String, String> prodHM = new HashMap<String, String>();
    
    static {
    	
    	// populate product hashmap
    	// This table is needed because the product names in the actual granules
    	// do not match those in the XML Product Profiles, which appear to be 
    	// a short-hand version of the longer names.  Swell.
    	// Mapping:  Product name in granule -> Product name in XML Product Profile
    	
    	// XXX TJJ - need to rework how this hashmap is used in NPP Data Source code
    	// at present, we only want a map entry if a "Factors" variable is present
    	// (meaning scales/offsets are used).  Why did I just notice this? (20 Sep 2011)
    	// beats me...
    	
    	prodHM.put("Albedo", "Albedo");
    	prodHM.put("AntennaTemperature", "antennaTemperature");
    	prodHM.put("BrightnessTemperature", "BrightnessTemperature");
    	prodHM.put("BulkSeaSurfaceTemperature", "bulkSST");
    	prodHM.put("Chlorophyll_a", "Chlorophyll_a");
    	prodHM.put("CloudBaseHeightLayer", "cbhLyr");
    	prodHM.put("CloudBaseHeightTotal", "cbhTot");
    	prodHM.put("IST_Array", "IST Pixel");
    	prodHM.put("Land_Temp", "lst");
    	prodHM.put("NumberOfAggregatedImageryPixels", "scdNumAggPix");
    	prodHM.put("Radiance", "Radiance");
    	prodHM.put("Reflectance", "Reflectance");
    	prodHM.put("SceneCounts", "sceneCounts");
    	prodHM.put("SkinSeaSurfaceTemperature", "skinSST");
    	prodHM.put("SnowFraction", "scdFractionFromBinaryMap");
    	prodHM.put("TOA_NDVI", "TOA_NDVI");
    	prodHM.put("TOC_EVI", "TOC_EVI");
    	prodHM.put("VegetationFraction", "VegetationFraction");
    	
    }
    
    /**
     * Map the NPP product name to those used in the XML Product Profiles,
     * which seem to be short-hand versions of the long names.
     * 
     * @param prodName
     * @return product id for the corresponding XML Product Profile
     */
    
    public static String mapProdNameToProfileName(String prodName) {
    	String s = null;
    	if (prodHM != null) {
    		s = (String) prodHM.get(prodName);
    	}
    	return s;
    }
    
    /**
	 * Return true if the input string is a valid NPP Product
     * 
     * @param prodName
     * @return true if the input string is a valid NPP Product
     */
    
    public static boolean isValidNPPProduct(String prodName) {
    	if (prodName == null) return false;
    	if (prodHM != null) {
    		if (prodHM.containsValue(prodName)) {
    			return true;
    		}
    	}
    	return false;
    }
	
}
