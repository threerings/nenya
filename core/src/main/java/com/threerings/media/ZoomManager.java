package com.threerings.media;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ZoomManager {

    protected static final double ZOOM_SNAP_THRESHOLD = 0.05;
    protected static final double[] DEFAULT_ZOOM_STEPS = { 0.25, 0.3333, 0.5, 1.0, 2.0 };

    protected double _zoomLevel = 1.0;
    protected double _minZoomLevel = 0.25;
    protected double _maxZoomLevel = 2.0;
    protected double _zoomSteps[] = DEFAULT_ZOOM_STEPS;

    public interface ZoomListener {

        void zoomChanged(double oldZoom, double newZoom);
    }

    private final List<ZoomListener> _listeners = new CopyOnWriteArrayList<>();

    public ZoomManager() {
    }

    public ZoomManager(double zoomLevel) {
        _zoomLevel = zoomLevel;
    }

    public ZoomManager(double zoomLevel, double minZoomLevel, double maxZoomLevel) {
        _zoomLevel = zoomLevel;
        _minZoomLevel = minZoomLevel;
        _maxZoomLevel = maxZoomLevel;
    }

    /**
     * Returns the center point of the given rectangle.
     *
     * @param rect the rectangle to find the center of
     * @return the center point of the rectangle
     */
    public static Point center(Rectangle rect) {
        int centerX = rect.x + rect.width / 2;
        int centerY = rect.y + rect.height / 2;
        return new Point(centerX, centerY);
    }

    /**
     * Rescales the given bounds rectangle based on the current zoom level.
     * Useful when the zoom level changes to update the viewport.
     *
     * @param viewport the rectangle to rescale
     * @param width    the new width
     * @param height   the new height
     * @return a new rectangle representing the rescaled bounds
     */
    public Rectangle rescaleBounds(Rectangle viewPort, int width, int height) {
        Point center = center(viewPort);
        int newWidth = (int) (width / _zoomLevel);
        int newHeight = (int) (height / _zoomLevel);
        return new Rectangle(center.x - newWidth / 2, center.y - newHeight / 2, newWidth, newHeight);
    }

    /**
     * Scales the given viewport rectangle based on the current zoom level,
     * centering the scaling on the viewport's center.
     *
     * @param viewPort the viewport rectangle to scale
     * @return a new rectangle representing the scaled viewport
     */
    public Rectangle scaleOnCenter(Rectangle viewPort) {
        Point center = center(viewPort);
        int newWidth = (int) (viewPort.width / _zoomLevel);
        int newHeight = (int) (viewPort.height / _zoomLevel);
        return new Rectangle(center.x - newWidth / 2, center.y - newHeight / 2, newWidth, newHeight);
    }

    /**
     * Adjusts the given mouse event for the current zoom level. Uses scaled
     * distance from the center of the panel
     *
     * @param event the mouse event to adjust
     */
    public void adjustMouseEvent(MouseEvent event, Rectangle viewPort) {
        Point adjustedPoint = screenToVirtual(viewPort, event.getPoint());
        event.translatePoint(
                adjustedPoint.x - event.getX(),
                adjustedPoint.y - event.getY());
    }

    /**
     * Adjusts the given point for the current zoom level. Zoom happens from the
     * center of the viewport.
     *
     * @param viewPort the viewport rectangle
     * @param toAdjust the point to adjust
     * @return a new point representing the adjusted coordinates
     */
    public Point screenToVirtual(Rectangle viewPort, Point screenPoint) {
        Point center = center(viewPort);
        double scale = _zoomLevel;
        int virtX = (int) ((screenPoint.x - center.x) / scale + center.x);
        int virtY = (int) ((screenPoint.y - center.y) / scale + center.y);
        return new Point(virtX, virtY);
    }

    /**
     * Sets the zoom steps to use when zooming in and out.
     *
     * @param zoomSteps the zoom steps to set
     */
    public void setZoomSteps(double[] zoomSteps) {
        _zoomSteps = zoomSteps;
    }

    /**
     * Creates a mouse wheel listener that adjusts the zoom level.
     *
     * @return a mouse wheel listener
     */
    public MouseWheelListener createMouseWheelListener() {
        return e -> {
            int closestStepIndex = 0;
            double smallestDiff = Math.abs(_zoomLevel - _zoomSteps[0]);
            for (int i = 0; i < _zoomSteps.length; i++) {
                double diff = Math.abs(_zoomLevel - _zoomSteps[i]);
                if (diff < smallestDiff) {
                    smallestDiff = diff;
                    closestStepIndex = i;
                }
            }

            int notches = e.getWheelRotation();
            int newIndex = closestStepIndex + (notches > 0 ? -1 : 1);
            if (newIndex < 0) {
                newIndex = 0;
            } else if (newIndex >= _zoomSteps.length) {
                newIndex = _zoomSteps.length - 1;
            }
            setZoomLevel(_zoomSteps[newIndex]);
        };
    }

    public double getZoomLevel() {
        return _zoomLevel;
    }

    /**
     * Scales the current zoom level by the given factor.
     *
     * @param scale the scale factor
     * @return the actual scale factor applied after clamping to min and max
     */
    public double scaleZoomLevel(double scale) {
        double oldZoom = _zoomLevel;
        setZoomLevel(_zoomLevel * scale);
        return _zoomLevel / oldZoom;
    }

    /**
     * Sets the current zoom level, clamping it to the min and max zoom levels.
     *
     * @param zoomLevel the new zoom level
     */
    public void setZoomLevel(double zoomLevel) {
        double oldZoom = _zoomLevel;
        if (zoomLevel < _minZoomLevel) {
            _zoomLevel = _minZoomLevel;
        } else if (zoomLevel > _maxZoomLevel) {
            _zoomLevel = _maxZoomLevel;
        } else {
            _zoomLevel = zoomLevel;
            // if we're close to 1.0, we might as well snap to it
            if (Math.abs(_zoomLevel - 1.0) < ZOOM_SNAP_THRESHOLD) {
                _zoomLevel = 1.0;
            }
        }

        if (oldZoom != _zoomLevel) {
            fireZoomChanged(oldZoom, _zoomLevel);
        }
    }

    public double getMaxZoomLevel() {
        return _maxZoomLevel;
    }

    public void setMaxZoomLevel(double maxZoomLevel) {
        _maxZoomLevel = maxZoomLevel;
    }

    public double getMinZoomLevel() {
        return _minZoomLevel;
    }

    public void setMinZoomLevel(double minZoomLevel) {
        _minZoomLevel = minZoomLevel;
    }

    public void addZoomListener(ZoomListener listener) {
        _listeners.add(listener);
    }

    public void removeZoomListener(ZoomListener listener) {
        _listeners.remove(listener);
    }

    private void fireZoomChanged(double oldZoom, double newZoom) {
        for (ZoomListener listener : _listeners) {
            listener.zoomChanged(oldZoom, newZoom);
        }
    }
}
