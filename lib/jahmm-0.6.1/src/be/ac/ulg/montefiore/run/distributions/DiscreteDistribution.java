/* jahmm package - v0.6.1 */

/*
 *  Copyright (c) 2004-2006, Jean-Marc Francois.
 *
 *  This file is part of Jahmm.
 *  Jahmm is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Jahmm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jahmm; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */

package be.ac.ulg.montefiore.run.distributions;

import java.io.*;


/** 
 * This interface must be implemented by all the package's classes implementing
 * a discrete random distribution.  Distributions are not mutable.
 */
public interface DiscreteDistribution 
extends Serializable
{    
    /**
     * Generates a pseudo-random number.  The numbers generated by this function
     * are drawn according to the pseudo-random distribution described by the
     * object that implements it.
     *
     * @return A pseudo-random number.
     */
    public int generate();


    /**
     * Returns the probability of a given number.
     *
     * @param n An integer.
     */
    public double probability(int n);
}
