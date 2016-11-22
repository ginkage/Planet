#pragma version(1)
#pragma rs java_package_name(com.ginkage.planet)
#pragma rs_fp_relaxed

float rotateY = 0.f;
float lightY = 0.f;

rs_sampler gLinear;
rs_allocation gPlanetMap;
rs_allocation gNormalMap;
rs_allocation gPlanet;
rs_allocation gNormal;

uchar4 RS_KERNEL rotation(uchar4 in, uint32_t x, uint32_t y)
{
	float sx = x / 512.0 - 1;
	float sy = 1 - y / 512.0;
	float z2 = 1.0 - sx * sx - sy * sy;
	float4 result = { 0, 0, 0, 0 };

	if (z2 > 0.0) {
		float sz = sqrt(z2);
		float2 vCoord = { 0, 0 };

		if (fabs(sz) > fabs(sy)) {
			vCoord.x = atan2(sqrt((float)(1.0 - sy*sy - sx*sx)), -sx) / (2.0 * M_PI);
			vCoord.y = acos(sy) / M_PI;
			if (sz < 0.0) { vCoord.x = 1.0 - vCoord.x; }
		}
		else {
			vCoord.x = atan2(sz, -sx) / (2.0 * M_PI);
			vCoord.y = acos(sqrt((float)(1.0 - sz*sz - sx*sx))) / M_PI;
			if (sz < 0.0) { vCoord.x = 1.0 + vCoord.x; }
			if (sy < 0.0) { vCoord.y = 1.0 - vCoord.y; }
		}

		vCoord.x += rotateY;
		result = rsSample(gPlanetMap, gLinear, vCoord);

		float3 zz = { 0, 0, 1 };
		float4 norm = rsSample(gNormalMap, gLinear, vCoord);
		float3 vNorm = normalize(2.0 * norm.xyz - 1.0 + zz);
		float sin_theta = -sy;
		float cos_theta = sqrt((float)(1.0 - sy * sy));
		float sin_phi = sx / cos_theta;
		float cos_phi = sz / cos_theta;

		float3 vRot = {
			vNorm.x * cos_phi + (-vNorm.y * sin_theta + vNorm.z * cos_theta) * sin_phi,
			-vNorm.y * cos_theta - vNorm.z * sin_theta,
			-vNorm.x * sin_phi + (-vNorm.y * sin_theta + vNorm.z * cos_theta) * cos_phi,
		};

		norm.xyz = (vRot + 1.0) * 0.5;
		rsSetElementAt_uchar4(gNormal, rsPackColorTo8888(norm), x, y);
	}

	return rsPackColorTo8888(result);
}

uchar4 RS_KERNEL lighting(uchar4 in, uint32_t x, uint32_t y)
{
	float sx = x / 512.0 - 1;
	float sy = 1 - y / 512.0;
	float4 result = { 0, 0, 0, 0 };

	if (sx * sx + sy * sy < 1.0) {
		uchar4 in2 = rsGetElementAt_uchar4(gNormal, x, y);
		float3 vNorm = 2.0 * rsUnpackColor8888(in2).xyz - 1.0;
		float3 vCol = rsUnpackColor8888(in).xyz;
		float light = -vNorm.x * sin(lightY) + vNorm.z * cos(lightY);
		result.xyz = light * 1.25 * vCol;
		result.w = 1;
	}

	return rsPackColorTo8888(result);
}
