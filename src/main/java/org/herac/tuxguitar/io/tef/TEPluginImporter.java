package org.herac.tuxguitar.io.tef;

import org.herac.tuxguitar.gui.system.plugins.base.TGImporterPlugin;
import org.herac.tuxguitar.io.base.TGRawImporter;

public class TEPluginImporter extends TGImporterPlugin {

  public String getAuthor() {
    return "Julian Casadesus <julian@casadesus.com.ar>";
  }

  public String getDescription() {
    return "TEF file format importer";
  }

  protected TGRawImporter getImporter() {
    return new TESongImporter();
  }

  public String getName() {
    return "TEF file format importer";
  }

  public String getVersion() {
    return "1.0";
  }
}
