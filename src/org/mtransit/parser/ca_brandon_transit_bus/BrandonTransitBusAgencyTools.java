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
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		matcher.find();
		return Long.parseLong(matcher.group());
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			matcher.find();
			return matcher.group();
		}
		return super.getRouteShortName(gRoute);
	}

	private static final String RLN_20 = "City Circular 20";
	private static final String RLN_21 = "City Circular 21";
	private static final String RSN_20_STARTS_WITH = "20";
	private static final String RSN_21_STARTS_WITH = "21";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (gRoute.getRouteShortName().startsWith(RSN_20_STARTS_WITH)) {
			return RLN_20;
		} else if (gRoute.getRouteShortName().startsWith(RSN_21_STARTS_WITH)) {
			return RLN_21;
		}
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

	private static final String COLOR_EC222C = "EC222C";
	private static final String COLOR_F9D66A = "F9D66A";
	private static final String COLOR_8F1A8F = "8F1A8F";
	private static final String COLOR_F033A3 = "F033A3";
	private static final String COLOR_28BAF2 = "28BAF2";
	private static final String COLOR_2E3192 = "2E3192";
	private static final String COLOR_7DCC2B = "7DCC2B";
	private static final String COLOR_416025 = "416025";
	private static final String COLOR_A83E28 = "A83E28";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return COLOR_EC222C;
			case 4: return COLOR_F9D66A;
			case 5: return COLOR_8F1A8F;
			case 6: return COLOR_F033A3;
			case 9: return COLOR_28BAF2;
			case 10: return COLOR_2E3192;
			case 11: return COLOR_A83E28;
			// @formatter:on
			}
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		matcher.find();
		int rsn = Integer.parseInt(matcher.group());
		switch (rsn) {
		// @formatter:off
		case 20: return COLOR_7DCC2B;
		case 21: return COLOR_416025;
		// @formatter:on
		}
		System.out.printf("\nUnexpected route color for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final String DOWNTOWN_TERMINAL = "Downtown Terminal";
	private static final String WEST = "West";
	private static final String SOUTH = "South";
	private static final String TRANS_CANADA = "TransCanada";
	private static final String BRHA = "BRHA";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1l, new RouteTripSpec(1l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2036", "2030", "1" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1", "2010", "2036" })) //
				.compileBothTripSort());
		map2.put(4l, new RouteTripSpec(4l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TRANS_CANADA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1", "1051", "1056" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1056", "1065", "1" })) //
				.compileBothTripSort());
		map2.put(6l, new RouteTripSpec(6l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRHA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3056", "3065", "1" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1", "3083", "3056" })) //
				.compileBothTripSort());
		map2.put(9l, new RouteTripSpec(9l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3001", "3090", "1" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1", "2096", "2001" })) //
				.compileBothTripSort());
		map2.put(10l, new RouteTripSpec(10l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2004", "3076", "1" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1", "2092", "2012" })) //
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

	private static final String W = "W";
	private static final String E = "E";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 5l) {
			mTrip.setHeadsignString(cleanTripHeadsign(mRoute.getLongName()), 0);
			return;
		} else if (mRoute.getId() == 11l) {
			mTrip.setHeadsignString(cleanTripHeadsign(mRoute.getLongName()), 0);
			return;
		} else if (mRoute.getId() == 20l) {
			if (gTrip.getRouteId().endsWith(E)) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getRouteId().endsWith(W)) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 21l) {
			if (gTrip.getRouteId().endsWith(E)) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getRouteId().endsWith(W)) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		}
		System.out.printf("\n%s: Unexptected trip %s!", mRoute.getId(), gTrip);
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
