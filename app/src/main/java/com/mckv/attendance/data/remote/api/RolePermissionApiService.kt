package com.mckv.attendance.data.remote.api

import com.mckv.attendance.data.remote.dto.request.LoginRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RolePermissionApiService {

    @GET("api/role/permissions")
    fun getAllPermissionForRoles(
        @Query("roles") roles: List<String>,
        @Header("X-INTERNAL-SECRET") secret: String
    ): Call<ResponseBody>
}