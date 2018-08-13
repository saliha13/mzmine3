package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

public class LipidCoreClass extends AbstractLipidClass {

  public enum CoreClasses {
    FATTYACYLS, //
    GLYCEROLIPIDS, //
    GLYCEROPHOSPHOLIPIDS;//

    // var
    CoreClasses() {

    }

  }

  LipidCoreClass(String name, String abbr) {
    super(name, abbr);
  }

}
