package com.minimind.app.network.model

import com.google.gson.annotations.SerializedName

data class DatasetInfo(
    val id: String,
    val name: String,
    val size: Long,
    @SerializedName("sample_count") val sampleCount: Int
)

data class DatasetListResponse(
    val datasets: List<DatasetInfo>
)

data class DatasetPreviewResponse(
    val samples: List<String>,
    @SerializedName("total_count") val totalCount: Int
)

data class DatasetDeleteRequest(
    @SerializedName("dataset_id") val datasetId: String
)

data class DatasetUploadRequest(
    @SerializedName("dataset_name") val datasetName: String,
    val format: String = "jsonl"
)
