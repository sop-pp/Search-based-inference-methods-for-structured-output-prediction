package com.structuredoutputclassifiers.data;

import cc.mallet.types.InstanceList;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;

/**
 * Author: Marcin Dobrowolski
 */
public abstract class DatasetReader {

    public InstanceListWithCoreFeatures read(String filePath) {
        return read(filePath, 0);
    }

    public abstract InstanceListWithCoreFeatures read(String filePath, int numBack);
}
