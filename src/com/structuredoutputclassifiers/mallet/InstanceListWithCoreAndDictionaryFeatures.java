package com.structuredoutputclassifiers.mallet;

import cc.mallet.types.Alphabet;

/**
 * Author: Marcin Dobrowolski
 */
public class InstanceListWithCoreAndDictionaryFeatures extends InstanceListWithCoreFeatures {

    protected Alphabet dictionaryFeatures;

    public InstanceListWithCoreAndDictionaryFeatures(Alphabet dataAlphabet, Alphabet targetAlphabet, Alphabet
            coreFeatures, Alphabet additionalFeatures, Alphabet dictionaryFeatures) {
        super(dataAlphabet, targetAlphabet, coreFeatures, additionalFeatures);
        this.dictionaryFeatures = dictionaryFeatures;
    }
}
