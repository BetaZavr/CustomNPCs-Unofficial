package noppes.npcs.util;

public class ValueUtil {

	public static float correctFloat(float given, float min, float max) { return Math.min(Math.max(given, min), max); }

	public static float wrapFloat(float given, float min, float max) {
		float range = max - min;
		if (range <= 0.0f) { return min; }
		float result = (given - min) % range;
		if (result < 0.0f) { result += range; }
		return result + min;
	}

	public static float wrapRadians(float given) {
		return wrapFloat(given, (float) -Math.PI, (float) Math.PI);
	}

	public static double correctDouble(double given, double min, double max) { return Math.min(Math.max(given, min), max); }

	public static int correctInt(int given, int min, int max) { return Math.min(Math.max(given, min), max); }

	public static long correctLong(long given, long min, long max) { return Math.min(Math.max(given, min), max); }

	public static float onlyPositiveFloat(float given, float max) {
		if (given < 0) { given *= -1; }
		if (max > 0) { given = Math.min(given, max); }
		return given;
	}

	public static double onlyPositiveDouble(double given, double max) {
		if (given < 0) { given *= -1; }
		if (max > 0) { given = Math.min(given, max); }
		return given;
	}

	public static int onlyPositiveInt(int given, int max) {
		if (given < 0) { given *= -1; }
		if (max > 0) { given = Math.min(given, max); }
		return given;
	}

	public static double max(double... obj) {
		if (obj == null || obj.length == 0) { return Double.MAX_VALUE; }
		double max = obj[0];
		for (double i : obj) {
			if (i > max) { max = i; }
		}
		return max;
	}

	public static double max(float... obj) {
		if (obj == null || obj.length == 0) { return Float.MAX_VALUE; }
		float max = obj[0];
		for (float i : obj) {
			if (i > max) { max = i; }
		}
		return max;
	}

	public static int max(int... obj) {
		if (obj == null || obj.length == 0) { return Integer.MAX_VALUE; }
		int max = obj[0];
		for (int i : obj) {
			if (i > max) { max = i; }
		}
		return max;
	}

	public static double min(double... obj) {
		if (obj == null || obj.length == 0) { return Double.MIN_VALUE; }
		double min = obj[0];
		for (double i : obj) {
			if (i < min) { min = i; }
		}
		return min;
	}

	public static double min(float... obj) {
		if (obj == null || obj.length == 0) { return Float.MIN_VALUE; }
		float min = obj[0];
		for (float i : obj) {
			if (i < min) { min = i; }
		}
		return min;
	}

	public static int min(int... obj) {
		if (obj == null || obj.length == 0) { return Integer.MIN_VALUE; }
		int min = obj[0];
		for (int i : obj) {
			if (i < min) { min = i; }
		}
		return min;
	}

	public static double lerp(double percent, double min, double max) {
		return min + percent * (max - min);
	}

}
