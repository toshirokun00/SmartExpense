package com.smartexpense.domain.usecase

import com.smartexpense.domain.repository.ExpenseRepository

class UploadReceiptPhotoUseCase(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(localUri: String): String? =
        expenseRepository.uploadReceiptPhoto(localUri)
}
