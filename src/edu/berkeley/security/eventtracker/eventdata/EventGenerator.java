package edu.berkeley.security.eventtracker.eventdata;

import java.util.Collection;
import java.util.Random;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A generator for creating a sequence of random events.
 */
public class EventGenerator {
	private long mDelta;
	private long mStartTime;
	private long mVariability;
	private Random random = new Random();
	private Multimap<String, String> mTagEvents;
	private boolean mIsFirstTime = true;

	/**
	 * Constructs a new {@link EventGenerator} that creates event spaced apart
	 * by {@code eventDeltaMillis} milliseconds.
	 * 
	 * @param eventDeltaMillis
	 *            how far apart (approximately) events should be spaced
	 */
	public EventGenerator(long eventDeltaMillis) {
		mDelta = eventDeltaMillis;
		mStartTime = System.currentTimeMillis();
		mVariability = mDelta / 2;
		mTagEvents = generateEventTags();
	}

	/**
	 * Generates a {@link Multimap} of tags mapped to events.
	 * 
	 * @return the multimap
	 */
	private Multimap<String, String> generateEventTags() {
		Multimap<String, String> tagEventMap = HashMultimap.create();
		tagEventMap.put("sleep", "sleeping");
		tagEventMap.put("sleep", "napping");

		tagEventMap.put("food", "lunch");
		tagEventMap.put("food", "dinner");
		tagEventMap.put("food", "breakfast");
		tagEventMap.put("food", "snacking");

		tagEventMap.put("class", "cs194-cloud");
		tagEventMap.put("class", "ee122");
		tagEventMap.put("class", "gamescrafters");

		tagEventMap.put("work", "working");

		tagEventMap.put("fun", "movie");
		tagEventMap.put("fun", "party");
		tagEventMap.put("fun", "rock climbing");
		tagEventMap.put("fun", "video games");

		tagEventMap.put("other", "tutoring");
		return tagEventMap;
	}

	/**
	 * Generates a random {@link EventEntry}. Each subsequent call generates an
	 * event further into the future.
	 * 
	 * @return a new random event
	 */
	public EventEntry generateEvent() {
		EventEntry event = new EventEntry();
		event.mTag = generateTag();
		event.mName = generateName(event.mTag);
		long time1 = getRandomTime();
		long time2 = getRandomTime();
		event.mStartTime = Math.min(time1, time2);
		event.mEndTime = Math.max(time1, time2);
		if (mIsFirstTime) {
			mIsFirstTime = false;
			event.mEndTime = Math.min(mStartTime, event.mEndTime);
		}
		mStartTime -= mDelta;
		return event;
	}

	/**
	 * Generates a random tag.
	 * 
	 * @return the tag
	 */
	private String generateTag() {
		return randomElement(mTagEvents.keys());
	}

	/**
	 * Generates a random event name corresponding to the given tag.
	 * 
	 * @param tag
	 *            the tag of the event
	 * @return the event name
	 */
	private String generateName(String tag) {
		return randomElement(mTagEvents.get(tag));
	}

	/**
	 * Chooses a random element of the given collection.
	 * 
	 * @param collection
	 *            the collection to choose from
	 * @return a random element
	 */
	private <T> T randomElement(Collection<T> collection) {
		int randIndex = random.nextInt(collection.size());
		for (T element : collection) {
			if (randIndex == 0) {
				return element;
			}
			randIndex--;
		}
		throw new RuntimeException("Should not reach.");
	}

	/**
	 * Generates random time close to the current start time.
	 * 
	 * @return the time in Unix time
	 */
	private long getRandomTime() {
		return (long) (random.nextDouble() * mVariability) + mStartTime;
	}

	public static void main(String[] args) {
		int ONE_DAY = 1000 * 60 * 60 * 24;
		EventGenerator eg = new EventGenerator(ONE_DAY);
		EventEntry event;
		while (true) {
			event = eg.generateEvent();
			System.out.println(event);
		}
	}
}
