package org.processmining.sapm.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/*
 * Copyright © 2009-2014 The Apromore Initiative.
 * 
 * This file is part of "Apromore".
 * 
 * "Apromore" is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * "Apromore" is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.joda.time.DateTime;
import org.processmining.sapm.stagemining.groundtruth.GroundTruth;

public class LogUtils {

	public static String getConceptName(XAttributable attrib) {
		String name = XConceptExtension.instance().extractName(attrib);
		return (name != null ? name : "<no name>");
	}

	public static void setConceptName(XAttributable attrib, String name) {
		XConceptExtension.instance().assignName(attrib, name);
	}

	public static String getLifecycleTransition(XEvent event) {
		String name = XLifecycleExtension.instance().extractTransition(event);
		return (name != null ? name : "<no transition>");
	}

	public static void setLifecycleTransition(XEvent event, String transition) {
		XLifecycleExtension.instance().assignTransition(event, transition);
	}

	public static void setTimestamp(XEvent event, Date timestamp) {
		XTimeExtension.instance().assignTimestamp(event, timestamp);
	}

	public static DateTime getTimestamp(XEvent event) {
		Date date = XTimeExtension.instance().extractTimestamp(event);
		return new DateTime(date);
	}

	public static String getOrganizationalResource(XEvent event) {
		String name = XOrganizationalExtension.instance().extractResource(event);
		return (name != null ? name : "<no resource>");
	}

	public static String getValue(XAttribute attr) {
		if (attr instanceof XAttributeBoolean) {
			Boolean b = ((XAttributeBoolean) attr).getValue();
			return b.toString();
		} else if (attr instanceof XAttributeContinuous) {
			Double d = ((XAttributeContinuous) attr).getValue();
			return d.toString();
		} else if (attr instanceof XAttributeDiscrete) {
			Long l = ((XAttributeDiscrete) attr).getValue();
			return l.toString();
		} else if (attr instanceof XAttributeLiteral) {
			String s = ((XAttributeLiteral) attr).getValue();
			return s;
		} else if (attr instanceof XAttributeTimestamp) {
			Date d = ((XAttributeTimestamp) attr).getValue();
			return d.toString();
		}
		return "";
	}
	
	public static XLog copyLog(XLog log) {
        XFactoryNaiveImpl factory = new XFactoryNaiveImpl();
        XLog newLog = factory.createLog(log.getAttributes());
        for (XTrace trace : log) {
            XTrace newTrace = factory.createTrace(trace.getAttributes());
            for (XEvent e : trace) {
            	XEvent newEvent = factory.createEvent(e.getAttributes());
            	newTrace.add(newEvent);
            }
            newLog.add(newTrace);
        }
        return newLog;
	}
	
//	/**
//	 * Remember: this log has been pre-processed by 
//	 * adding one starting event and one ending event
//	 * Compute the number of distinct events by ending events
//	 * @return: map from event name to number of distinct event before it in a trace
//	 * key: ending event name, value: number of distinct event before the key in a trace
//	 * Note: if one event is never an ending event in a trace, it will NOT
//	 * be in the returning result.
//	 */
//	public static Map<String,Double> computeDistinctEventsStatToEnd(XLog log) {
//		Map<String,Double> result = new HashMap<String,Double>();
//		Map<String,Integer> countOccurs = new HashMap<String,Integer>();
//		Set<String> visited = new HashSet<String>();
//		for (XTrace trace : log) {
//			//Count the number of distinct event in a trace
//			visited.clear();
//			for (XEvent evt : trace) {
//				visited.add(LogUtils.getConceptName(evt).toLowerCase());
//			}
//			
//			//Cumulatively add the number of distinct events in different traces
//			XEvent last = trace.get(trace.size()-2); //exclude the added ending event
//			String lastName = LogUtils.getConceptName(last).toLowerCase();
//			if (!countOccurs.containsKey(lastName)) {
//				countOccurs.put(lastName, 1);
//			}
//			else {
//				countOccurs.put(lastName, countOccurs.get(lastName) + 1);
//			}
//			
//			if (!result.containsKey(lastName)) {
//				result.put(lastName, 1.0*visited.size()-2); //exclude the added starting and ending event
//			}
//			else {
//				result.put(lastName, visited.size() - 2 + result.get(lastName));
//			}
//		}
//		
//		//Compute the average number of distinct events
//		for (String eventName : result.keySet()) {
//			result.put(eventName, 1.0*result.get(eventName)/countOccurs.get(eventName));
//		}
//		
//		System.out.println("Avg. Distinct Events per Trace (end_event->number): " + result.toString());
//		
//		return result;
//	}
//	
//	public static Map<String,Double> computeDistinctEventsStatFromStart(XLog log) {
//		Map<String,Double> result = new HashMap<String,Double>();
//		Map<String,Integer> countOccurs = new HashMap<String,Integer>();
//		Set<String> visited = new HashSet<String>();
//		for (XTrace trace : log) {
//			//Count the number of distinct event in a trace
//			visited.clear();
//			for (XEvent evt : trace) {
//				visited.add(LogUtils.getConceptName(evt).toLowerCase());
//			}
//			
//			//Cumulatively add the number of distinct events in different traces
//			XEvent first = trace.get(1); //exclude the added start event
//			String firstName = LogUtils.getConceptName(first).toLowerCase();
//			if (!countOccurs.containsKey(firstName)) {
//				countOccurs.put(firstName, 1);
//			}
//			else {
//				countOccurs.put(firstName, countOccurs.get(firstName) + 1);
//			}
//			
//			if (!result.containsKey(firstName)) {
//				result.put(firstName, 1.0*visited.size()-2); //exclude the added starting event
//			}
//			else {
//				result.put(firstName, visited.size() - 2 + result.get(firstName));
//			}
//		}
//		
//		//Compute the average number of distinct events
//		for (String eventName : result.keySet()) {
//			result.put(eventName, 1.0*result.get(eventName)/countOccurs.get(eventName));
//		}
//		
//		System.out.println("Distinct Events per Trace (start_event->number): " + result.toString());
//		
//		return result;
//	}
//	
//	/**
//	 * Select the event that is the ending event of a trace and 
//	 * has the maximum number of distict events before it in a trace
//	 * @param log
//	 * @return: event name
//	 */
//	public static String computeMainstreamEndingEventName(XLog log, boolean fromStart) {
//		Map<String,Double> mapDistinctEvent = null;
//		if (fromStart) {
//			mapDistinctEvent = LogUtils.computeDistinctEventsStatFromStart(log);
//		}
//		else {
//			mapDistinctEvent = LogUtils.computeDistinctEventsStatToEnd(log);
//		}
//		double maxCount = 0;
//		String maxName = "";
//		for (String eventName : mapDistinctEvent.keySet()) {
//			if (mapDistinctEvent.get(eventName) > maxCount) {
//				maxCount = mapDistinctEvent.get(eventName);
//				maxName = eventName;
//			}
//		}
//		return maxName;
//	}
	
//	public static int getDistinctEventClassCount(XLog log) {
//		Set<String> eventSet = new HashSet<String>();
//		for (XTrace trace : log) {
//			for (XEvent e : trace) {
//				eventSet.add(LogUtils.getConceptName(e).toLowerCase());
//			}
//		}
//		return eventSet.size();
//	}
	
	/**
	 * Add start and end events to all traces of an input log
	 * Return a new log, the old log must be unchanged
	 * @param log
	 * @throws ParseException
	 */
	public static XLog addStartEndEvents(XLog log, String startName, String endName) throws ParseException {
		//--------------------------------
		// Create start/end event
		//--------------------------------
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy"); 
		XFactory factory = new XFactoryNaiveImpl();
		XLog newLog = factory.createLog(log.getAttributes());
		
		XEvent startEvent = factory.createEvent();
    	XAttributeMap startEventMap = factory.createAttributeMap();
    	startEventMap.put("concept:name", factory.createAttributeLiteral("concept:name", startName, null));
    	startEventMap.put("lifecycle:transition", factory.createAttributeLiteral("lifecycle:transition", "complete", null));
    	startEventMap.put("time:timestamp", factory.createAttributeTimestamp("time:timestamp", df.parse("01/01/1970"), null));
    	startEvent.setAttributes(startEventMap);	
    	
		XEvent endEvent = factory.createEvent();
    	XAttributeMap endEventMap = factory.createAttributeMap();
    	endEventMap.put("concept:name", factory.createAttributeLiteral("concept:name", endName, null));
    	endEventMap.put("lifecycle:transition", factory.createAttributeLiteral("lifecycle:transition", "complete", null));
    	endEventMap.put("time:timestamp", factory.createAttributeTimestamp("time:timestamp", df.parse("01/01/2020"), null));
    	endEvent.setAttributes(endEventMap);	
    	
    	for (XTrace trace : log) {
    		XTrace newTrace = factory.createTrace(trace.getAttributes());
    		for (XEvent e : trace) {
    			newTrace.add(e);
    		}
			newTrace.add(0, startEvent);
			newTrace.add(endEvent);
			newLog.add(newTrace);
    	}
    	
    	return newLog;
	}
	
//	public static boolean containClass(XLogInfo logInfo, String eventName) {
//		for (XEventClass eventClass : logInfo.getEventClasses().getClasses()) {
//			if (eventClass.getId().toLowerCase().equals(eventName.toLowerCase())) {
//				return true;
//			}
//		}
//		return false;
//	}
	
//	public static Map<String,Set<XTrace>> buildKeyTraceMap(XLog log) {
//		Map<String,Set<XTrace>> mapKeyTrace = new HashMap<>();
//		for (XTrace trace : log) {
//			String key = "";
//			for (XEvent e : trace) key += LogUtils.getConceptName(e) + "|";
//			if (!mapKeyTrace.containsKey(key)) mapKeyTrace.put(key, new HashSet<XTrace>());
//			mapKeyTrace.get(key).add(trace);
//		}
//		return mapKeyTrace;
//	}
	
	/**
	 * Filter log by trace clustering, i.e. only keep clusters
	 * with number of traces higher than a threshold 
	 * This is because when a stage decomposition is created, the number of 
	 * trace clusters tend to decrease due to there are more similar (shorter) traces
	 * Thus, traces that are infrequent in stage sublogs should be removed
	 * @param log
	 * @param threshold
	 * @return
	 * @throws Exception 
	 */
//	public static XLog filterByTraceClustering(XLog log, double threshold) throws Exception {
//		Map<String,Integer> mapKeyToCount = new HashMap<>(); //number of traces in the cluster
//		
//		//First, construct trace clustering
//		for (XTrace trace : log) {
//			String key = "";
//			for (XEvent e : trace) {
//				key += LogUtils.getConceptName(e) + "|";
//			}
//			if (!mapKeyToCount.containsKey(key)) {
//				mapKeyToCount.put(key, 1);
//			}
//			else {
//				mapKeyToCount.put(key, mapKeyToCount.get(key) + 1);
//			}
//		}
//		
//		//Then, check every cluster against the threshold
//		Set<XTrace> tobeRemoved = new HashSet<>();
//		for (XTrace trace : log) {
//			String key = "";
//			for (XEvent e : trace) {
//				key += LogUtils.getConceptName(e) + "|";
//			}
//			if (mapKeyToCount.containsKey(key)) {
//				if (1.0*mapKeyToCount.get(key)/log.size() < threshold) {
//					tobeRemoved.add(trace);
//				}
//			}
//			else {
//				throw new Exception("Something wrong! Cannot find traces with key=" + key);
//			}
//		}
//		
//		//Create new log
//		XFactoryNaiveImpl factory = new XFactoryNaiveImpl();
//		XLog newLog = factory.createLog(log.getAttributes());
//		for (XTrace trace : log) {
//			if (!tobeRemoved.contains(trace) ) {
//				newLog.add(trace);
//			}
//		}
//		
//		return newLog;
//	}
	
//	public static XLog filterOutTraces(XLog log, Set<String> traceIDs) {
//		XFactory factory = new XFactoryNaiveImpl();
//		XLog filteredLog = factory.createLog(log.getAttributes());
//		for (XTrace trace : log) {
//			if (!traceIDs.contains(LogUtils.getConceptName(trace))) {
//				filteredLog.add(trace);
//			}
//		}
//		return filteredLog;
//	}
	
	public static XLog filterOutWholeTraceByRelations(XLog log, Set<String> relations) {
		XFactory factory = new XFactoryNaiveImpl();
		XLog filteredLog = factory.createLog(log.getAttributes());
		for (XTrace trace : log) {
			XTrace newTrace = factory.createTrace(trace.getAttributes()); 
			
			// trace string
			List<String> traceList = new ArrayList<>();
			for (int i=0;i<trace.size();i++) {
				traceList.add(LogUtils.getConceptName(trace.get(i)));
				newTrace.add(trace.get(i));
			}
			
			// only select whole trace if contains no relations
			boolean found = false;
			for (String relation : relations) {
				List<String> relationList = new ArrayList<String>(Arrays.asList(relation.split("@")));
				if (Collections.indexOfSubList(traceList , relationList) != -1) {
					found = true;
					break;
				}
			}
			if (!found) filteredLog.add(newTrace);
		}
		
		// Filter out traces with event classes in relations which are not found in the log
		// because these event classes will be harmful to precision. These relations
		// are reasoning errors of base miners
//		Set<String> wrongEventClasses = new HashSet<>();
//		for (String relation : relations) {
//			if (!foundRelations.contains(relation)) {
//				wrongEventClasses.add(relation.split("@")[1]);
//			}
//		}
//		XLog filteredLog2 = LogUtils.filterOutWholeTraceByEventClasses(filteredLog, wrongEventClasses);
		
		return filteredLog;
	}
	
//	public static XLog filterOutRelations(XLog log, Set<String> relationStrings) {
//		XFactory factory = new XFactoryNaiveImpl();
//		XLog filteredLog = factory.createLog(log.getAttributes());
//		
//		for (XTrace trace : log) {
//			String traceString = "";
//			for (int i=0;i<trace.size();i++) {
//				if (i!=trace.size()-1) {
//					traceString += LogUtils.getConceptName(trace.get(i)) + "@";
//				}
//				else {
//					traceString += LogUtils.getConceptName(trace.get(i));
//				}
//			}
//			
//			// Mark all relations if found in the trace
//			BitSet traceBits = new BitSet(trace.size()); //all bits are false;
//			for (String relationString : relationStrings) {
//				int k = relationString.split("@").length; // k classes, e.g. k=2
//				if (traceString.contains(relationString)) {
//					for (int i=0;i<trace.size();i++) {
//						if ((i+k-1) < trace.size()) {
//							boolean match = true;
//							for (int j=i;j<=(i+k-1);j++) {
//								if (!relationString.contains(LogUtils.getConceptName(trace.get(j)))) match = false;
//							}
//							if (match) {
//								for (int j=i;j<=(i+k-1);j++) {
//									traceBits.set(j); // true means contain the class
//								}
//							}
//						}
//					}
//				}
//			}
//			
//			// only take unmarked events
//			XTrace filteredTrace = factory.createTrace(trace.getAttributes()); 
//			for (int i=0;i<trace.size();i++) {
//				if (!traceBits.get(i)) filteredTrace.add(trace.get(i));
//			}
//			
//			// not take empty traces
//			if (!filteredTrace.isEmpty() &&
//					!(filteredTrace.size()==1 && LogUtils.getConceptName(filteredTrace.get(0)).equals("start")) &&
//					!(filteredTrace.size()==2 && LogUtils.getConceptName(filteredTrace.get(0)).equals("start") && 
//							LogUtils.getConceptName(filteredTrace.get(1)).equals("end"))) {
//				filteredLog.add(filteredTrace);
//			}
//		}
//		 
//		
//		return filteredLog;
//	}
//	
//	public static XLog filterOutEventClasses(XLog log, Set<String> tobeRemoved) {
//		XFactory factory = new XFactoryNaiveImpl();
//		XLog newLog = factory.createLog(log.getAttributes());
//		for (XTrace trace : log) {
//			XTrace newTrace = factory.createTrace(trace.getAttributes());
//			for (XEvent e : trace) {
//				if (!tobeRemoved.contains(LogUtils.getConceptName(e))) newTrace.add(e);
//			}
//			if (!newTrace.isEmpty() &&
//					!(newTrace.size()==1 && newTrace.get(0).equals("start")) &&
//					!(newTrace.size()==2 && newTrace.get(0).equals("start") && newTrace.get(0).equals("end"))) {
//				newLog.add(newTrace);
//			}
//		}
//		return newLog;
//	}
	
	public static XLog filterOutWholeTraceByEventClasses(XLog log, Set<String> tobeRemoved) {
		XFactory factory = new XFactoryNaiveImpl();
		XLog newLog = factory.createLog(log.getAttributes());
		for (XTrace trace : log) {
			XTrace newTrace = factory.createTrace(trace.getAttributes());
			boolean selected = true;
			for (XEvent e : trace) {
				newTrace.add(e);
				if (tobeRemoved.contains(LogUtils.getConceptName(e))) {
					selected = false;
					break;
				}
			}
			if (selected) newLog.add(newTrace);
		}
		return newLog;
	}
	
	public static double compareWithLogGroundTruth(List<Set<String>> activitySetList, XLog log) throws Exception {
		String logName = LogUtils.getConceptName(log);
		
		String gtClassName = "";
		if (logName.contains("BPI12")) {
			gtClassName = "org.processmining.sapm.stagemining.groundtruth.StageModelBPI2012";
		}
		else if (logName.contains("BPI13")) {
			gtClassName = "org.processmining.sapm.stagemining.groundtruth.StageModelBPI2013";
		}
		else if (logName.contains("BPIC15")) {
			gtClassName = "org.processmining.sapm.stagemining.groundtruth.StageModelBPI2015";
		}
		else if (logName.contains("BPI17")) {
			gtClassName = "org.processmining.sapm.stagemining.groundtruth.StageModelBPI2017";
		}
		else {
			throw new Exception("The used log has no ground truth of stages");
		}

		GroundTruth gtClass = (GroundTruth)Class.forName(gtClassName).newInstance();
		double gtIndex = Measure.computeMeasure(activitySetList, gtClass.getGroundTruth(log), 2); //fowls-mallows
		return gtIndex;
	}
	
}