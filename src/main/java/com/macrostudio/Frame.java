package com.macrostudio;

import java.util.List;

/** State of one game tick. {@code k} is only stored when the set of held keys changed. */
public final class Frame {
	public float y;
	public float p;
	public int s;
	public int[] k;
	public List<Ev> e;
}
