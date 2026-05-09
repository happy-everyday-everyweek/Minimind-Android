package com.minimind.app.network

import com.minimind.app.network.model.*
import retrofit2.http.*

interface ApiService {

    @POST("/api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("/api/models")
    suspend fun getModels(): ModelListResponse

    @GET("/api/models/weights")
    suspend fun getModelWeights(): ModelListResponse

    @DELETE("/api/models/{modelId}")
    suspend fun deleteModel(@Path("modelId") modelId: String): Map<String, String>

    @POST("/api/models/export")
    suspend fun exportModel(@Body request: ModelExportRequest): Map<String, String>

    @POST("/api/models/download")
    suspend fun downloadModel(@Body request: ModelDownloadRequest): Map<String, String>

    @GET("/api/datasets")
    suspend fun getDatasets(): DatasetListResponse

    @GET("/api/datasets/{datasetId}/preview")
    suspend fun previewDataset(@Path("datasetId") datasetId: String): DatasetPreviewResponse

    @DELETE("/api/datasets/{datasetId}")
    suspend fun deleteDataset(@Path("datasetId") datasetId: String): Map<String, String>

    @POST("/api/training/pretrain")
    suspend fun startPretrain(@Body config: PretrainConfig): TrainingStartResponse

    @POST("/api/training/sft")
    suspend fun startSft(@Body config: SftConfig): TrainingStartResponse

    @POST("/api/training/lora")
    suspend fun startLora(@Body config: LoraConfig): TrainingStartResponse

    @POST("/api/training/dpo")
    suspend fun startDpo(@Body config: DpoConfig): TrainingStartResponse

    @POST("/api/training/ppo")
    suspend fun startPpo(@Body config: PpoConfig): TrainingStartResponse

    @POST("/api/training/grpo")
    suspend fun startGrpo(@Body config: GrpoConfig): TrainingStartResponse

    @POST("/api/training/agent")
    suspend fun startAgent(@Body config: AgentConfig): TrainingStartResponse

    @POST("/api/training/distillation")
    suspend fun startDistillation(@Body config: DistillationConfig): TrainingStartResponse

    @GET("/api/training/status/{taskId}")
    suspend fun getTrainingStatus(@Path("taskId") taskId: String): TrainingStatus

    @POST("/api/training/pause/{taskId}")
    suspend fun pauseTraining(@Path("taskId") taskId: String): Map<String, String>

    @POST("/api/training/resume/{taskId}")
    suspend fun resumeTraining(@Path("taskId") taskId: String): Map<String, String>

    @POST("/api/training/stop/{taskId}")
    suspend fun stopTraining(@Path("taskId") taskId: String): Map<String, String>

    @GET("/api/health")
    suspend fun checkHealth(): Map<String, String>

    @POST("/api/initialize")
    suspend fun initialize(): Map<String, String>

    @POST("/api/restart")
    suspend fun restart(): Map<String, String>

    @POST("/api/test_connection")
    suspend fun testConnection(@Body config: Map<String, String>): Map<String, String>
}
