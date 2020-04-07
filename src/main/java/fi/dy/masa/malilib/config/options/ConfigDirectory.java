package fi.dy.masa.malilib.config.options;

import java.io.File;
import fi.dy.masa.malilib.config.ConfigType;

public class ConfigDirectory extends ConfigFile
{
    public ConfigDirectory(String name, File defaultValue, String comment)
    {
        super(ConfigType.DIRECTORY, name, defaultValue, comment);
    }
}
