package com.tollfinder.routing;

import com.tollfinder.domain.DrivingRoute;
import com.tollfinder.domain.Pincode;

public interface RouteProvider {
    DrivingRoute findRoute(Pincode source, Pincode destination);
}
