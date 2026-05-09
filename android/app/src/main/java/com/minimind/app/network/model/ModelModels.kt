package com.minimind.app.network.model

import com.google.gson.annotations.SerializedName

data class ModelWeight(
    val id: String,
    val name: String,
    val size: Long,
    val source: String,
    @SerializedName("created_at") val createdAt: String
)

data class ModelListResponse(
    val models: List<ModelWeight>
)

data class ModelDeleteRequest(
    @SerializedName("model_id") val modelId: String
)

data class ModelExportRequest(
    @SerializedName("model_id") val modelId: String,
    @SerializedName("export_path") val exportPath: String
)

data class ModelDownloadRequest(
    @SerializedName("model_url") val modelUrl: String,
    @SerializedName("model_name") val modelName: String
)
