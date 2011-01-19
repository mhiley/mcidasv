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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;

import visad.FlatField;
import visad.Set;

public class CrIS_SDR_SwathAdapter extends SwathAdapter {

   public CrIS_SDR_SwathAdapter() {
   }

   public CrIS_SDR_SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
     super(reader, metadata);
   }

   protected void setLengths() {
     int len = getTrackLength();
     setTrackLength(len *= 3);
     len = getXTrackLength();
     setXTrackLength( len *= 3);
   }

   public FlatField getData(Object subset) throws Exception {

     Set domainSet = makeDomain(subset);

     HashMap new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
     double[] new_coords = new double[] {0.0, coords[1]/3, 1.0};

     new_subset.put(SwathAdapter.track_name, new_coords);
     new_coords = new double[] {0.0, (30.0 - 1.0), 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     new_coords = new double[] {0.0, (9.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     return makeFlatField(domainSet, new_subset);
   }

   public float[] processRange(float[] values, Object subset) {
                double[] track_coords = (double[]) ((HashMap)subset).get(SwathAdapter.track_name);
                double[] xtrack_coords = (double[]) ((HashMap)subset).get(SwathAdapter.xtrack_name);

                int numElems = ((int)(xtrack_coords[1] - xtrack_coords[0]) + 1);
                int numLines = ((int)(track_coords[1] - track_coords[0]) + 1);

                values = CrIS_SDR_Utility.psuedoScanReorder(values, 90, numLines*3);

                //- subset here, if necessary

                return values;
   }
}