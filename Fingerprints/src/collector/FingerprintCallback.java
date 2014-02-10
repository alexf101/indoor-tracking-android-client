package collector;

import datatypes.Fingerprint;

import java.util.List;

public interface FingerprintCallback {

    /**
     * A function to be called by FingerprintCollector when it has finished collecting ALL samples.
     *
     * Each sample is one fingerprint in the list.
     *
     * @param fingerprint A list of samples (fingerprints) collected.
     */
    public void onSamplesCollected(List<Fingerprint> fingerprint);

    /**
     * A function to be called by FingerprintCollector when it has finished collecting ONE sample.
     * @param newFingerprint
     */
    public void onSampleCollected(Fingerprint newFingerprint);
}
