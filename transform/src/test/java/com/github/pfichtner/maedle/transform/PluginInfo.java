package com.github.pfichtner.maedle.transform;

public class PluginInfo {

	public String pluginId;
	public String taskName;
	public String extensionName;

	public PluginInfo(String pluginId, String taskName, String extensionName) {
		this.pluginId = pluginId;
		this.taskName = taskName;
		this.extensionName = extensionName;
	}

}
