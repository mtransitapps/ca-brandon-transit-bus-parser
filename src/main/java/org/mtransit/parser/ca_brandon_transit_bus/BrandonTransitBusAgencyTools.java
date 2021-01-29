package org.mtransit.parser.ca_brandon_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://opendata.brandon.ca/
// http://opendata.brandon.ca/Transit/google_transit.zip
public class BrandonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-brandon-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BrandonTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Brandon Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Brandon Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
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
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				return matcher.group();
			}
		}
		return super.getRouteShortName(gRoute);
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "00B8F1"; // BLUE (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
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
		throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
	}

	private static final String DOWNTOWN_TERMINAL = "Downtown Term";
	private static final String TRANS_CANADA = "TransCanada";

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TRANS_CANADA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(//
								"1", // Downtown Terminal
								"23", // <> Princess Avenue @ 8th Street
								"24", // ++
								"1051", // ++
								"1056" // ++
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(//
								"1056", // ++
								"1065", // ++
								"22", // ++
								"3106" // Downtown Terminal (Return)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "ACC N", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(//
								"1", // Downtown Terminal
								"18", // ++
								"3077" // ACC North Campus
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(//
								"3077", // ACC North Campus
								"1009", // ++
								"16", // Rosser Avenue @ 10th Street
								"3106" // Downtown Terminal (Return)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(8L, new RouteTripSpec(8L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Shoppers Mall") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(//
								"2007", // Behind Shoppers Mall
								"2095", // ++
								"16", // Rosser Avenue @ 10th Street
								"3106" // Downtown Terminal (Return)
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(//
								"1", // Downtown Terminal
								"2084", // ++
								"2007" // Behind Shoppers Mall
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(14L, new RouteTripSpec(14L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Wankling Blvd") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(//
								"2036", // Wankling Blvd @ Richmond Avenue
								"1044", // ++
								"35", // 13th Street @ Princess Avenue
								"3106" // Downtown Terminal (Return)
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(//
								"1", // Downtown Terminal
								"32", // ++
								"2036" // Wankling Blvd @ Richmond Avenue
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(15L, new RouteTripSpec(15L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "ACC", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(//
								"1", // Downtown Terminal
								"3055", // ++
								"3062" // Assiniboine Community College
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(//
								"3062", // Assiniboine Community College
								"3068", // ++
								"23", // Princess Avenue @ 8th Street
								"3106" // Downtown Terminal (Return)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(16L, new RouteTripSpec(16L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Neelin", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Shoppers Mall") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(//
								"3001", // Sobey's South
								"2033", // ++ Shoppers Mall
								"3097" // Neelin High School
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(//
								"3097", // Neelin High School
								"2027", // ++
								"2099", // Richmond Terminal (south)
								"3107" // Sobey's South (Return)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(17L, new RouteTripSpec(17L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Shoppers Mall") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(//
								"2033", // Shoppers Mall
								"3008", // ++
								"38", // 10th Street @ Lorne Avenue
								"3106" // Downtown Terminal (Return)
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(//
								"1", // Downtown Terminal
								"2088", // ++
								"2033" // Shoppers Mall
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(23L, new RouteTripSpec(23L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Crocus") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(//
								"2056", // Crocus Plains HS
								"3047", // ++
								"23", // Princess Avenue @ 8th Street
								"3106" // Downtown Terminal (Return)
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(//
								"1", // Downtown Terminal
								"3089", // ++
								"2056" // Crocus Plains HS
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public boolean directionFinderEnabled() {
		return false; // because direction ID (& trip head-sign) not provided
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 19L) {
			mTrip.setHeadsignString( //
					"Downtown Term", //
					gTrip.getDirectionId() == null ? 0 : gTrip.getDirectionId() //
			);
			return;
		}
		if (mRoute.getId() == 22L) {
			mTrip.setHeadsignString( //
					"Downtown Term", //
					gTrip.getDirectionId() == null ? 0 : gTrip.getDirectionId() //
			);
			return;
		}
		throw new MTLog.Fatal("%s: Unexpected trip %s!", mRoute.getId(), gTrip);
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern AND_SLASH = Pattern.compile("((^|\\W)([&@])(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String AND_SLASH_REPLACEMENT = "$2/$4";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = AND_SLASH.matcher(gStopName).replaceAll(AND_SLASH_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
