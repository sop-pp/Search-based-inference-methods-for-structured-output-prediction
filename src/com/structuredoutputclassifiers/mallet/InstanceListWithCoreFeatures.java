package com.structuredoutputclassifiers.mallet;

import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;

/**
 * Author: Marcin Dobrowolski
 */
public class InstanceListWithCoreFeatures extends InstanceList {

    public InstanceListWithCoreFeatures(Alphabet dataAlphabet, Alphabet targetAlphabet, Alphabet coreFeatures, Alphabet additionalFeatures) {
        super(dataAlphabet, targetAlphabet);
        this.coreFeatures = coreFeatures;
    }

    public InstanceListWithCoreFeatures(Alphabet dataAlphabet, Alphabet targetAlphabet, Alphabet coreFeatures) {
        this(dataAlphabet, targetAlphabet, coreFeatures, null);
    }

    protected Alphabet coreFeatures;
    protected Alphabet additionalFeatures;

    public Alphabet getCoreFeatures() {
        return coreFeatures;
    }

    public void setCoreFeatures(Alphabet coreFeatures) {
        this.coreFeatures = coreFeatures;
    }

    public Alphabet getAdditionalFeatures() {
        return additionalFeatures;
    }

    public void setAdditionalFeatures(Alphabet additionalFeatures) {
        this.additionalFeatures = additionalFeatures;
    }
}
