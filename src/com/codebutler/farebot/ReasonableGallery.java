/*
 * ReasonableGallery.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// http://stackoverflow.com/questions/2373617/how-to-stop-scrolling-in-a-gallery-widget

package com.codebutler.farebot;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.Gallery;

public class ReasonableGallery extends Gallery{
    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        return false;
    }

    public ReasonableGallery(Context context) {
        super(context);
    }

    public ReasonableGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReasonableGallery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
      public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        setAnimationDuration(600);
        return super.onScroll(e1, e2, distanceX, distanceY);
      }

      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float velMax = 2500f;
        float velMin = 1000f;
        float velX = Math.abs(velocityX);
        if (velX > velMax) {
          velX = velMax;
        } else if (velX < velMin) {
          velX = velMin;
        }
        velX -= 600;
        int k = 500000;
        int speed = (int) Math.floor(1f / velX * k);
        setAnimationDuration(speed);

        int kEvent;
        if (isScrollingLeft(e1, e2)) {
          // Check if scrolling left
          kEvent = KeyEvent.KEYCODE_DPAD_LEFT;
        } else {
          // Otherwise scrolling right
          kEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
        }
        onKeyDown(kEvent, null);

        return true;
      }

    private boolean isScrollingLeft(MotionEvent e1, MotionEvent e2){
      return e2.getX() > e1.getX();
    }

//

//    @Override
//    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        return false;
//    }
}
