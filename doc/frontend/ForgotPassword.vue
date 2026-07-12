<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-900 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8 bg-slate-800/80 backdrop-blur-md p-8 rounded-2xl shadow-xl border border-slate-700">
      
      <!-- Header -->
      <div class="text-center">
        <h2 class="text-3xl font-extrabold text-white tracking-tight">
          Quên mật khẩu?
        </h2>
        <p class="mt-2 text-sm text-slate-400">
          Nhập email đăng ký để nhận liên kết khôi phục thông tin tài khoản của bạn.
        </p>
      </div>

      <!-- Form -->
      <form class="mt-8 space-y-6" @submit.prevent="handleSubmit">
        
        <!-- Alerts -->
        <div v-if="successMessage" class="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 p-4 rounded-xl text-sm flex items-start space-x-2">
          <svg class="h-5 w-5 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{{ successMessage }}</span>
        </div>

        <div v-if="errorMessage" class="bg-rose-500/10 border border-rose-500/20 text-rose-400 p-4 rounded-xl text-sm flex items-start space-x-2">
          <svg class="h-5 w-5 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Input Field -->
        <div class="rounded-md shadow-sm">
          <div>
            <label for="email-address" class="block text-sm font-medium text-slate-300 mb-2">Địa chỉ Email</label>
            <input
              id="email-address"
              name="email"
              type="email"
              required
              v-model="email"
              :disabled="submitting"
              class="appearance-none relative block w-full px-4 py-3 border border-slate-700 bg-slate-900 placeholder-slate-500 text-white rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm transition-all duration-200"
              placeholder="nhanvien@example.com"
            />
          </div>
        </div>

        <!-- Submit Button -->
        <div>
          <button
            type="submit"
            :disabled="submitting || successMessage"
            class="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-xl text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span class="absolute left-0 inset-y-0 flex items-center pl-3">
              <svg 
                v-if="!submitting"
                class="h-5 w-5 text-indigo-400 group-hover:text-indigo-300 transition-colors" 
                fill="none" 
                viewBox="0 0 24 24" 
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
              <!-- Spinner -->
              <svg v-else class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            {{ submitting ? 'Đang gửi yêu cầu...' : 'Gửi liên kết khôi phục' }}
          </button>
        </div>

        <!-- Back to Login Link -->
        <div class="text-center mt-4">
          <router-link to="/login" class="text-sm font-medium text-indigo-400 hover:text-indigo-300 transition-colors">
            Quay lại Đăng nhập
          </router-link>
        </div>

      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const email = ref('')
const submitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const API_BASE_URL = 'https://ais-dev-pw4m2b4tdeexlu2cu2osje-420049508645.asia-east1.run.app'

const handleSubmit = async () => {
  if (!email.value) return
  
  submitting.value = ref(true)
  successMessage.value = ''
  errorMessage.value = ''

  try {
    const response = await axios.get(`${API_BASE_URL}/api/auth/recover-account/${encodeURIComponent(email.value)}`)
    
    if (response.data && response.data.status === 'success') {
      successMessage.value = response.data.data || 'Đường dẫn khôi phục tài khoản đã được gửi về email của bạn.'
    } else {
      errorMessage.value = 'Có lỗi xảy ra trong quá trình xử lý.'
    }
  } catch (error) {
    if (error.response && error.response.data && error.response.data.message) {
      errorMessage.value = error.response.data.message
    } else {
      errorMessage.value = 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.'
    }
  } finally {
    submitting.value = false
  }
}
</script>
