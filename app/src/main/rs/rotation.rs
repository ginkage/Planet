/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma version(1)
#pragma rs java_package_name(com.ginkage.planet)
#pragma rs_fp_relaxed

float rotateX = 0.f;

rs_sampler gLinear;
rs_allocation gPlanet;
rs_allocation gNormalMap;

/*
    RenderScript kernel that performs saturation manipulation.
*/
uchar4 RS_KERNEL rotation(uchar4 in, uint32_t x, uint32_t y)
{
	float sx = x / 512.0 - 1;
	float sy = 1 - y / 512.0;
	float z2 = 1.0 - sx * sx - sy * sy;
	float4 result = { 0, 0, 0, 1 };

	if (z2 > 0.0) {
		float sz = sqrt(z2);
		float y = sy;
		float z = sz;
		float2 vCoord = { 0, 0 };

		if (fabs(z) > fabs(y)) {
			vCoord.x = atan2(sqrt((float)(1.0 - y*y - sx*sx)), -sx) / (2.0 * M_PI);
			vCoord.y = acos(y) / M_PI;
			if (z < 0.0) { vCoord.x = 1.0 - vCoord.x; }
		}
		else {
			vCoord.x = atan2(z, -sx) / (2.0 * M_PI);
			vCoord.y = acos(sqrt((float)(1.0 - z*z - sx*sx))) / M_PI;
			if (z < 0.0) { vCoord.x = 1.0 + vCoord.x; }
			if (y < 0.0) { vCoord.y = 1.0 - vCoord.y; }
		}

		vCoord.x += rotateX;

		float3 vCol = rsSample(gPlanet, gLinear, vCoord).xyz;
		float3 vNorm = normalize(rsSample(gNormalMap, gLinear, vCoord).xyz - 0.5);
		float sin_theta = -sy;
		float cos_theta = sqrt((float)(1.0 - sy * sy));
		float sin_phi = sx / cos_theta;
		float cos_phi = sz / cos_theta;

		float3 vRot = {
		    vNorm.x * cos_phi + (-vNorm.y * sin_theta + vNorm.z * cos_theta) * sin_phi,
		    -vNorm.y * cos_theta - vNorm.z * sin_theta,
		    -vNorm.x * sin_phi + (-vNorm.y * sin_theta + vNorm.z * cos_theta) * cos_phi
		};

        result.xyz = vRot.z * vCol;
	}

    return rsPackColorTo8888(result);
}

