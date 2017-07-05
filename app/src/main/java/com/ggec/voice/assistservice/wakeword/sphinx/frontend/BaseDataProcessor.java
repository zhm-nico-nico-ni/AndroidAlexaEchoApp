/*
 * Copyright 2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package com.ggec.voice.assistservice.wakeword.sphinx.frontend;


/**
 * An abstract DataProcessor implementing elements common to all concrete DataProcessors, such as name, predecessor, and
 * timer.
 */
public abstract class BaseDataProcessor implements DataProcessor {

    private DataProcessor predecessor;

    public BaseDataProcessor() {
    }

    /**
     * Returns the processed Data output.
     *
     * @return an Data object that has been processed by this DataProcessor
     * @throws DataProcessingException if a data processor error occurs
     */
    public abstract Data getData() throws DataProcessingException;


    /** Initializes this DataProcessor. This is typically called after the DataProcessor has been configured. */
    public void initialize() {
    }


    /**
     * Returns the predecessor DataProcessor.
     *
     * @return the predecessor
     */
    public DataProcessor getPredecessor() {
        return predecessor;
    }


    /**
     * Sets the predecessor DataProcessor. This method allows dynamic reconfiguration of the front end.
     *
     * @param predecessor the new predecessor of this DataProcessor
     */
    public void setPredecessor(DataProcessor predecessor) {
        this.predecessor = predecessor;
    }

    protected void initLogger(){}

    public void newProperties(PropertySheet ps) throws PropertyException{}
}
