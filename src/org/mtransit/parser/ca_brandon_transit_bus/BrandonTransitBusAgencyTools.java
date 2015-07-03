package org.mtransit.parser.ca_brandon_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
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
		if (Utils.isDigitsOnly(gRoute.route_short_name)) {
			return Long.parseLong(gRoute.route_short_name);
		}
		Matcher matcher = DIGITS.matcher(gRoute.route_short_name);
		matcher.find();
		return Long.parseLong(matcher.group());
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.route_short_name)) {
			Matcher matcher = DIGITS.matcher(gRoute.route_short_name);
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
		if (gRoute.route_short_name.startsWith(RSN_20_STARTS_WITH)) {
			return RLN_20;
		} else if (gRoute.route_short_name.startsWith(RSN_21_STARTS_WITH)) {
			return RLN_21;
		}
		String routeLongName = gRoute.route_long_name;
		routeLongName = MSpec.cleanNumbers(routeLongName);
		routeLongName = MSpec.cleanStreetTypes(routeLongName);
		return MSpec.cleanLabel(routeLongName);
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
		if (Utils.isDigitsOnly(gRoute.route_short_name)) {
			int rsn = Integer.parseInt(gRoute.route_short_name);
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
		Matcher matcher = DIGITS.matcher(gRoute.route_short_name);
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
	public HashSet<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			return ALL_ROUTE_TRIPS2.get(mRoute.id).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, HashSet<MTrip> splitTrips, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			RouteTripSpec rts = ALL_ROUTE_TRIPS2.get(mRoute.id);
			return splitTripStop(gTrip, gTripStop, gtfs, //
					rts.getBeforeAfterStopIds(0), //
					rts.getBeforeAfterStopIds(1), //
					rts.getBeforeAfterBothStopIds(0), //
					rts.getBeforeAfterBothStopIds(1), //
					rts.getTripId(0), //
					rts.getTripId(1), //
					rts.getAllBeforeAfterStopIds());
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, gtfs);
	}

	private Pair<Long[], Integer[]> splitTripStop(GTrip gTrip, GTripStop gTripStop, GSpec gtfs, List<String> stopIdsTowards1, List<String> stopIdsTowards2,
			List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, long tidTowardsStop1, long tidTowardsStop2, List<String> allBeforeAfterStopIds) {
		String beforeAfter = getBeforeAfterStopId(gtfs, gTrip, gTripStop, stopIdsTowards1, stopIdsTowards2, stopIdsTowardsBoth21, stopIdsTowardsBoth12,
				allBeforeAfterStopIds);
		if (stopIdsTowards1.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1 }, new Integer[] { gTripStop.getStopSequence() });
		} else if (stopIdsTowards2.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop2 }, new Integer[] { gTripStop.getStopSequence() });
		} else if (stopIdsTowardsBoth21.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop2, tidTowardsStop1 }, new Integer[] { 1, gTripStop.getStopSequence() });
		} else if (stopIdsTowardsBoth12.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1, tidTowardsStop2 }, new Integer[] { 1, gTripStop.getStopSequence() });
		}
		System.out.printf("\nUnexptected trip stop to split %s.\n", gTripStop);
		System.exit(-1);
		return null;
	}

	private String getBeforeAfterStopId(GSpec gtfs, GTrip gTrip, GTripStop gTripStop, List<String> stopIdsTowards1, List<String> stopIdsTowards2,
			List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, List<String> allBeforeAfterStopIds) {
		int gStopMaxSequence = -1;
		ArrayList<String> afterStopIds = new ArrayList<String>();
		ArrayList<Integer> afterStopSequence = new ArrayList<Integer>();
		ArrayList<String> beforeStopIds = new ArrayList<String>();
		ArrayList<Integer> beforeStopSequence = new ArrayList<Integer>();
		for (GStopTime gStopTime : gtfs.getStopTimes(gTrip.getTripId(), null, null)) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			if (allBeforeAfterStopIds.contains(gStopTime.getStopId())) {
				if (gStopTime.getStopSequence() < gTripStop.getStopSequence()) {
					beforeStopIds.add(gStopTime.getStopId());
					beforeStopSequence.add(gStopTime.getStopSequence());
				}
				if (gStopTime.getStopSequence() > gTripStop.getStopSequence()) {
					afterStopIds.add(gStopTime.getStopId());
					afterStopSequence.add(gStopTime.getStopSequence());
				}
			}
			if (gStopTime.getStopSequence() > gStopMaxSequence) {
				gStopMaxSequence = gStopTime.getStopSequence();
			}
		}
		if (allBeforeAfterStopIds.contains(gTripStop.getStopId())) {
			if (gTripStop.getStopSequence() == 1) {
				beforeStopIds.add(gTripStop.getStopId());
				beforeStopSequence.add(gTripStop.getStopSequence());
			}
			if (gTripStop.getStopSequence() == gStopMaxSequence) {
				afterStopIds.add(gTripStop.getStopId());
				afterStopSequence.add(gTripStop.getStopSequence());
			}
		}
		String beforeAfterStopIdCandidate = findBeforeAfterStopIdCandidate(gTripStop, stopIdsTowards1, stopIdsTowards2, stopIdsTowardsBoth21,
				stopIdsTowardsBoth12, afterStopIds, afterStopSequence, beforeStopIds, beforeStopSequence);
		if (beforeAfterStopIdCandidate != null) {
			return beforeAfterStopIdCandidate;
		}
		System.out.printf("\nUnexpected trip (befores:%s|afters:%s) %s", beforeStopIds, afterStopIds, gTrip);
		System.exit(-1);
		return null;
	}

	private static final String DASH = "-";
	private static final String ALL = "*";

	private String findBeforeAfterStopIdCandidate(GTripStop gTripStop, List<String> stopIdsTowards1, List<String> stopIdsTowards2,
			List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, ArrayList<String> afterStopIds, ArrayList<Integer> afterStopSequence,
			ArrayList<String> beforeStopIds, ArrayList<Integer> beforeStopSequence) {
		String beforeAfterStopIdCurrent;
		Pair<Integer, String> beforeAfterStopIdCandidate = null;
		String beforeStopId, afterStopId;
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
					int size = Math.max(afterStopSequence.get(a) - gTripStop.getStopSequence(), gTripStop.getStopSequence() - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = beforeStopId + DASH + ALL;
			if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
				int size = gTripStop.getStopSequence() - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
				int size = afterStopSequence.get(a) - gTripStop.getStopSequence();
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				if (gTripStop.getStopId().equals(beforeStopId) && gTripStop.getStopId().equals(afterStopId)) {
					continue;
				}
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
					int size = Math.max(afterStopSequence.get(a) - gTripStop.getStopSequence(), gTripStop.getStopSequence() - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = beforeStopId + DASH + ALL;
			if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
				int size = gTripStop.getStopSequence() - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
				int size = afterStopSequence.get(a) - gTripStop.getStopSequence();
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		return beforeAfterStopIdCandidate == null ? null : beforeAfterStopIdCandidate.second;
	}

	private static final String W = "W";
	private static final String E = "E";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			return; // split
		}
		if (mRoute.id == 5l) {
			mTrip.setHeadsignString(cleanTripHeadsign(mRoute.longName), 0);
			return;
		} else if (mRoute.id == 11l) {
			mTrip.setHeadsignString(cleanTripHeadsign(mRoute.longName), 0);
			return;
		} else if (mRoute.id == 20l) {
			if (gTrip.getRouteId().endsWith(E)) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getRouteId().endsWith(W)) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 21l) {
			if (gTrip.getRouteId().endsWith(E)) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getRouteId().endsWith(W)) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		}
		System.out.printf("\n%s: Unexptected trip %s!", mRoute.id, gTrip);
		System.exit(-1);
		return;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = MSpec.cleanNumbers(tripHeadsign);
		tripHeadsign = MSpec.cleanStreetTypes(tripHeadsign);
		return MSpec.cleanLabel(tripHeadsign);
	}

	private static final Pattern AND_SLASH = Pattern.compile("((^|\\W){1}(&|@)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String AND_SLASH_REPLACEMENT = "$2/$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = AND_SLASH.matcher(gStopName).replaceAll(AND_SLASH_REPLACEMENT);
		gStopName = MSpec.cleanNumbers(gStopName);
		gStopName = MSpec.cleanStreetTypes(gStopName);
		return MSpec.cleanLabel(gStopName);
	}

	public static class RouteTripSpec {

		private static final String DASH = "-";
		private static final String ALL = "*";

		private long routeId;
		private int directionId0;
		private int headsignType0;
		private String headsignString0;
		private int directionId1;
		private int headsignType1;
		private String headsignString1;

		public RouteTripSpec(long routeId, int directionId0, int headsignType0, String headsignString0, int directionId1, int headsignType1,
				String headsignString1) {
			this.routeId = routeId;
			this.directionId0 = directionId0;
			this.headsignType0 = headsignType0;
			this.headsignString0 = headsignString0;
			this.directionId1 = directionId1;
			this.headsignType1 = headsignType1;
			this.headsignString1 = headsignString1;
		}

		private ArrayList<String> allBeforeAfterStopIds = new ArrayList<String>();

		public ArrayList<String> getAllBeforeAfterStopIds() {
			return this.allBeforeAfterStopIds;
		}

		public long getTripId(int directionIndex) {
			switch (directionIndex) {
			case 0:
				return MTrip.getNewId(this.routeId, this.directionId0);
			case 1:
				return MTrip.getNewId(this.routeId, this.directionId1);
			default:
				System.out.printf("\ngetTripId() > Unexpected direction index: " + directionIndex);
				System.exit(-1);
				return -1l;
			}
		}

		private HashMap<Integer, ArrayList<String>> beforeAfterStopIds = new HashMap<Integer, ArrayList<String>>();

		public ArrayList<String> getBeforeAfterStopIds(int directionIndex) {
			switch (directionIndex) {
			case 0:
				if (!this.beforeAfterStopIds.containsKey(this.directionId0)) {
					this.beforeAfterStopIds.put(this.directionId0, new ArrayList<String>());
				}
				return this.beforeAfterStopIds.get(this.directionId0);
			case 1:
				if (!this.beforeAfterStopIds.containsKey(this.directionId1)) {
					this.beforeAfterStopIds.put(this.directionId1, new ArrayList<String>());
				}
				return this.beforeAfterStopIds.get(this.directionId1);
			default:
				System.out.printf("\ngetBeforeAfterStopIds() > Unexpected direction index: " + directionIndex);
				System.exit(-1);
				return null;
			}
		}

		private HashMap<Integer, ArrayList<String>> beforeAfterBothStopIds = new HashMap<Integer, ArrayList<String>>();

		public ArrayList<String> getBeforeAfterBothStopIds(int directionIndex) {
			switch (directionIndex) {
			case 0:
				if (!this.beforeAfterBothStopIds.containsKey(this.directionId0)) {
					this.beforeAfterBothStopIds.put(this.directionId0, new ArrayList<String>());
				}
				return this.beforeAfterBothStopIds.get(this.directionId0);
			case 1:
				if (!this.beforeAfterBothStopIds.containsKey(this.directionId1)) {
					this.beforeAfterBothStopIds.put(this.directionId1, new ArrayList<String>());
				}
				return this.beforeAfterBothStopIds.get(this.directionId1);
			default:
				System.out.printf("\ngetBeforeAfterBothStopIds() > Unexpected direction index: " + directionIndex);
				System.exit(-1);
				return null;
			}
		}

		private HashSet<MTrip> allTrips = null;

		public HashSet<MTrip> getAllTrips() {
			if (this.allTrips == null) {
				initAllTrips();
			}
			return this.allTrips;
		}

		private void initAllTrips() {
			this.allTrips = new HashSet<MTrip>();
			if (this.headsignType0 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString0, this.directionId0));
			} else if (this.headsignType0 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString0)));
			} else {
				System.out.printf("\nUnexpected trip type " + this.headsignType0 + " for " + this.routeId);
				System.exit(-1);
			}
			if (this.headsignType1 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString1, this.directionId1));
			} else if (this.headsignType1 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString1)));
			} else {
				System.out.printf("\nUnexpected trip type " + this.headsignType1 + " for " + this.routeId);
				System.exit(-1);
			}
		}

		public RouteTripSpec addTripSort(int directionId, List<String> sortedStopIds) {
			this.allSortedStopIds.put(directionId, sortedStopIds);
			ArrayList<String> beforeStopIds = new ArrayList<String>();
			String currentStopId = null;
			for (int i = 0; i < sortedStopIds.size(); i++) {
				currentStopId = sortedStopIds.get(i);
				for (int b = beforeStopIds.size() - 1; b >= 0; b--) {
					addFromTo(directionId, beforeStopIds.get(b), currentStopId);
				}
				beforeStopIds.add(currentStopId);
			}
			return this;
		}

		private HashMap<Integer, List<String>> allSortedStopIds = new HashMap<Integer, List<String>>();

		public RouteTripSpec compileBothTripSort() {
			List<String> sortedStopIds0 = this.allSortedStopIds.get(this.directionId0);
			List<String> sortedStopIds1 = this.allSortedStopIds.get(this.directionId1);
			for (int i0 = 0; i0 < sortedStopIds0.size(); i0++) {
				String stopId0 = sortedStopIds0.get(i0);
				for (int i1 = 0; i1 < sortedStopIds1.size(); i1++) {
					String stopId1 = sortedStopIds1.get(i1);
					if (stopId0.equals(stopId1) || //
							sortedStopIds0.contains(stopId1) || sortedStopIds1.contains(stopId0)) {
						continue;
					}
					addBothFromTo(this.directionId0, stopId0, stopId1);
					addBothFromTo(this.directionId1, stopId1, stopId0);
				}
			}
			return this;
		}

		public int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
			int directionId;
			if (MTrip.getNewId(this.routeId, this.directionId0) == ts1.getTripId()) {
				directionId = this.directionId0;
			} else if (MTrip.getNewId(this.routeId, this.directionId1) == ts1.getTripId()) {
				directionId = this.directionId1;
			} else {
				System.out.printf("\nUnexpected trip ID " + ts1.getTripId());
				System.exit(-1);
				return 0;
			}
			List<String> sortedStopIds = this.allSortedStopIds.get(directionId);
			if (!sortedStopIds.contains(ts1GStop.stop_code) || !sortedStopIds.contains(ts2GStop.stop_code)) {
				System.out.printf("\nUnexpected stop IDs " + ts1GStop.stop_code + " AND/OR " + ts2GStop.stop_code);
				System.out.printf("\nNot in sorted list: " + sortedStopIds);
				System.exit(-1);
				return 0;
			}
			int ts1StopIndex = sortedStopIds.indexOf(ts1GStop.stop_code);
			int ts2StopIndex = sortedStopIds.indexOf(ts2GStop.stop_code);
			System.out.printf("\nSorted using sorted list: " + sortedStopIds);
			return ts2StopIndex - ts1StopIndex;
		}

		public RouteTripSpec addALLFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfter(directionId, stopIdFrom + DASH + ALL);
			addBeforeAfter(directionId, ALL + DASH + stopIdTo);
			addBeforeAfter(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addAllFrom(int directionId, String stopIdFrom) {
			addBeforeAfter(directionId, stopIdFrom + DASH + ALL);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			return this;
		}

		public RouteTripSpec addAllTo(int directionId, String stopIdTo) {
			addBeforeAfter(directionId, ALL + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfter(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		private void addBeforeAfter(int directionId, String beforeAfterStopId) {
			if (!this.beforeAfterStopIds.containsKey(directionId)) {
				this.beforeAfterStopIds.put(directionId, new ArrayList<String>());
			}
			this.beforeAfterStopIds.get(directionId).add(beforeAfterStopId);
		}

		public RouteTripSpec addAllBothFrom(int directionId, String stopIdFrom) {
			addBeforeAfterBoth(directionId, stopIdFrom + DASH + ALL);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			return this;
		}

		public RouteTripSpec addAllBothTo(int directionId, String stopIdTo) {
			addBeforeAfterBoth(directionId, ALL + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addBothFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfterBoth(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		private void addBeforeAfterBoth(int directionId, String beforeAfterStopId) {
			if (!this.beforeAfterBothStopIds.containsKey(directionId)) {
				this.beforeAfterBothStopIds.put(directionId, new ArrayList<String>());
			}
			this.beforeAfterBothStopIds.get(directionId).add(beforeAfterStopId);
		}
	}
}
