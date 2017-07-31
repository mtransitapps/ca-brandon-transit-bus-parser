package org.mtransit.parser.ca_brandon_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://opendata.brandon.ca/
// http://opendata.brandon.ca/Transit/google_transit.zip
public class BrandonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-brandon-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BrandonTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Brandon Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Brandon Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public long getRouteId(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName());
		}
		if ("IND".equals(gRoute.getRouteShortName())) {
			return 9001L;
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			return Long.parseLong(matcher.group());
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				return matcher.group();
			}
		}
		return super.getRouteShortName(gRoute);
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "00B8F1"; // BLUE (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 4: return "409AED";
			case 5: return "A83800";
			case 8: return "F8E208"; // "FAEB52";
			case 14: return "960096";
			case 15: return "0070FF";
			case 16: return "66C7EB";
			case 17: return "FF00C4";
			case 22: return "FFAB00";
			case 23: return "73B373";
			// @formatter:on
			}
		}
		if ("IND".equals(gRoute.getRouteShortName())) {
			return "4F4C4C";
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			int rsn = Integer.parseInt(matcher.group());
			switch (rsn) {
			// @formatter:off
			// @formatter:on
			}
		}
		System.out.printf("\nUnexpected route color for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final String DOWNTOWN_TERMINAL = "Downtown Terminal";
	private static final String TRANS_CANADA = "TransCanada";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(4l, new RouteTripSpec(4l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TRANS_CANADA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1", // Downtown Terminal
								"23", // <> Princess Avenue @ 8th Street
								"24", // ++
								"1051", // ++
								"1056", // ++
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1056", // ++
								"1065", // ++
								"22", // ++
								"23", // <> Princess Avenue @ 8th Street
								"1", // Downtown Terminal
						})) //
				.compileBothTripSort());
		map2.put(5l, new RouteTripSpec(5l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "ACC North", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1", // Downtown Terminal
								"18", // ++
								"3077", // ACC North Campus
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"3077", // ACC North Campus
								"1009", // ++
								"16", // Rosser Avenue @ 10th Street
								"1", // Downtown Terminal
						})) //
				.compileBothTripSort());
		map2.put(8l, new RouteTripSpec(8l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Shoppers Mall") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2007", // Behind Shoppers Mall
								"2095", // ++
								"16", // Rosser Avenue @ 10th Street
								"1", // Downtown Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1", // Downtown Terminal
								"2084", // ++
								"2007", // Behind Shoppers Mall
						})) //
				.compileBothTripSort());
		map2.put(14l, new RouteTripSpec(14l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Wankling Blvd") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2036", // Wankling Blvd @ Richmond Avenue
								"1044", // ++
								"35", // 13th Street @ Princess Avenue
								"1", // Downtown Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1", // Downtown Terminal
								"32", // ++
								"2036", // Wankling Blvd @ Richmond Avenue
						})) //
				.compileBothTripSort());
		map2.put(15l, new RouteTripSpec(15l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "ACC", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1", // Downtown Terminal
								"3055", // ++
								"3062", // Assiniboine Community College
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3062", // Assiniboine Community College
								"3068", // ++
								"23", // Princess Avenue @ 8th Street
								"1", // Downtown Terminal
						})) //
				.compileBothTripSort());
		map2.put(16l, new RouteTripSpec(16l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Neelin", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Shoppers Mall") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"3001", // Sobey's South
								"2033", // ++ Shoppers Mall
								"3097", // Neelin High School
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3097", // Neelin High School
								"2027", //
								"2099", // Richmond Terminal (south)
						})) //
				.compileBothTripSort());
		map2.put(17l, new RouteTripSpec(17l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Shoppers Mall") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2033", // Shoppers Mall
								"3008", // ++
								"38", // 10th Street @ Lorne Avenue
								"1", // Downtown Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1", // Downtown Terminal
								"2088", // ++
								"2033", // Shoppers Mall
						})) //
				.compileBothTripSort());
		map2.put(22l, new RouteTripSpec(22l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Riverheights") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2066", // Governors Road (east side)
								"2079", // ++
								"16", // Rosser Avenue @ 10th Street
								"1", // Downtown Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1", // Downtown Terminal
								"1017", // ++
								"2066", // Governors Road (east side)
						})) //
				.compileBothTripSort());
		map2.put(23l, new RouteTripSpec(23l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Crocus") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2056", // Crocus Plains HS
								"3047", // ++
								"23", // Princess Avenue @ 8th Street
								"1", // Downtown Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1", // Downtown Terminal
								"3089", // ++
								"2056", // Crocus Plains HS
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		System.out.printf("\n%s: Unexptected trip %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
		return;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern AND_SLASH = Pattern.compile("((^|\\W){1}(&|@)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String AND_SLASH_REPLACEMENT = "$2/$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = AND_SLASH.matcher(gStopName).replaceAll(AND_SLASH_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
