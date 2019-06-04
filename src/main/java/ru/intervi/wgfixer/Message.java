package ru.intervi.wgfixer;

public enum Message {
	TOO_MANY_ARGS("too_many_args"), SAVE_FAIL("save_fail"),
	NO_PERMISSION("no_permission_command"), RELOAD_SUCCESS("reload_success"),

	WORLD_ALREADY_CHOSEN("world.already_chosen", "world"),
	WORLD_NO_ARG("world.no_arg"),
	WORLD_UNKNOWN("world.unknown", "world"),

	NAME_NO_ARG("name.no_arg"),
	NAME_UNKNOWN("name.unknown", "player"),

	REGION_NO_ARG("region.no_arg"),
	REGION_UNKNOWN("region.unknown", "region"),
	REGION_NO_PERMISSION("region.no_permission"),

	ACTION_REMOVEOWNER("action.removeowner", "player", "region"),
	ACTION_REMOVEMEMBER("action.removemember", "player", "region"),
	ACTION_ADDOWNER("action.addowner", "player", "region"),
	ACTION_ADDMEMBER("action.addmember", "player", "region");

	private final String location;
	private final String[] placeholders;

	Message(String location) {
		this.location = location;
		placeholders = new String[]{};
	}

	Message(String location, String... placeholders) {
		this.location = location;
		this.placeholders = new String[placeholders.length];
		int i=0;
		for(String placeholder : placeholders)
			this.placeholders[i++] = "{" + placeholder + "}";
	}

	public String getLocation() {
		return "messages." + location;
	}

	public String[] getPlaceholders() {
		return placeholders;
	}
}
