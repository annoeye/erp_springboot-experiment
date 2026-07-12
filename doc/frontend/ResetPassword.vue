<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-900 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8 bg-slate-800/80 backdrop-blur-md p-8 rounded-2xl shadow-xl border border-slate-700">
      
      <!-- State 1: Validating Token on Mount -->
      <div v-if="isValidating" class="text-center py-12 space-y-4">
        <svg class="animate-spin h-10 w-10 text-indigo-500 mx-auto" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p class="text-slate-400 text-sm">Đang xác thực liên kết khôi phục tài khoản...</p>
      </div>

      <!-- State 2: Token Invalid / Expired -->
      <div v-else-if="!isTokenValid" class="text-center py-8 space-y-6">
        <div class="inline-flex p-3 bg-rose-500/10 rounded-full text-rose-500 border border-rose-500/20">
          <svg class="h-10 w-10" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <div class="space-y-2">
          <h2 class="text-2xl font-bold text-white">Liên kết hết hạn hoặc không hợp lệ</h2>
          <p class="text-sm text-slate-400 max-w-sm mx-auto">
            Đường dẫn khôi phục này đã hết hạn (sau 10 phút) hoặc đã được sử dụng. Vui lòng gửi lại yêu cầu khôi phục mới.
          </p>
        </div>
        <div class="pt-4">
          <router-link
            to="/forgot-password"
            class="inline-flex items-center justify-center px-6 py-3 border border-transparent text-sm font-medium rounded-xl text-white bg-indigo-600 hover:bg-indigo-700 transition-colors w-full"
          >
            Yêu cầu liên kết mới
          </router-link>
        </div>
      </div>

      <!-- State 3: Valid Token - Display Password Reset Form -->
      <div v-else class="space-y-6">
        <!-- Header -->
        <div class="text-center">
          <h2 class="text-3xl font-extrabold text-white tracking-tight">
            Đặt lại mật khẩu
          </h2>
          <!-- Personalized Greeting -->
          <div v-if="userDto" class="mt-3 p-3 bg-slate-900/60 rounded-xl border border-slate-700/50 text-left">
            <p class="text-xs text-slate-400">Tài khoản khôi phục:</p>
            <p class="text-sm font-semibold text-indigo-400">{{ userDto.fullName }}</p>
            <p class="text-xs text-slate-500">{{ userDto.email }}</p>
          </div>
        </div>

        <!-- Form -->
        <form class="space-y-5" @submit.prevent="handleReset">
          
          <!-- Success / Error Banners -->
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

          <!-- New Password Input -->
          <div>
            <label for="new-password" class="block text-sm font-medium text-slate-300 mb-1.5">Mật khẩu mới</label>
            <input
              id="new-password"
              name="newPassword"
              type="password"
              required
              v-model="newPassword"
              :disabled="submitting || successMessage"
              class="appearance-none block w-full px-4 py-3 border border-slate-700 bg-slate-900 placeholder-slate-500 text-white rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm transition-all"
              placeholder="••••••••"
            />
          </div>

          <!-- Confirm Password Input -->
          <div>
            <label for="confirm-password" class="block text-sm font-medium text-slate-300 mb-1.5">Xác nhận mật khẩu</label>
            <input
              id="confirm-password"
              name="confirmPassword"
              type="password"
              required
              v-model="confirmPassword"
              :disabled="submitting || successMessage"
              class="appearance-none block w-full px-4 py-3 border border-slate-700 bg-slate-900 placeholder-slate-500 text-white rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm transition-all"
              placeholder="••••••••"
            />
          </div>

          <!-- Submit Button -->
          <div class="pt-2">
            <button
              type="submit"
              :disabled="submitting || successMessage"
              class="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-xl text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all duration-200 disabled:opacity-50"
            >
              <span class="absolute left-0 inset-y-0 flex items-center pl-3">
                <svg v-if="!submitting" class="h-5 w-5 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                <svg v-else class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              </span>
              {{ submitting ? 'Đang thực hiện đổi...' : 'Xác nhận đổi mật khẩu' }}
            </button>
          </div>

        </form>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const token = ref('')
const isValidating = ref(true)
const isTokenValid = ref(false)
const userDto = ref(null)

const newPassword = ref('')
const confirmPassword = ref('')
const submitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const API_BASE_URL = 'https://ais-dev-pw4m2b4tdeexlu2cu2osje-420049508645.asia-east1.run.app'

onMounted(async () => {
  token.value = route.query.token || ''

  if (!token.value) {
    isValidating.value = false
    isTokenValid.value = false
    return
  }

  try {
    const response = await axios.get(`${API_BASE_URL}/api/auth/validate-reset-token`, {
      params: { token: token.value }
    })

    if (response.data && response.data.status === 'success') {
      isTokenValid.value = true
      userDto.value = response.data.data
    } else {
      isTokenValid.value = false
    }
  } catch (error) {
    isTokenValid.value = false
  } finally {
    isValidating.value = false
  }
})

const handleReset = async () => {
  if (!newPassword.value || !confirmPassword.value) {
    errorMessage.value = 'Vui lòng nhập đầy đủ các trường.'
    return
  }

  if (newPassword.value.length < 8) {
    errorMessage.value = 'Mật khẩu mới phải dài tối thiểu 8 ký tự.'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Mật khẩu xác nhận không trùng khớp.'
    return
  }

  submitting.value = true
  successMessage.value = ''
  errorMessage.value = ''

  try {
    const response = await axios.post(
      `${API_BASE_URL}/api/auth/reset-password`,
      {
        newPassword: newPassword.value,
        confirmPassword: confirmPassword.value
      },
      {
        params: { code: token.value }
      }
    )

    if (response.data && response.data.status === 'success') {
      successMessage.value = 'Mật khẩu đã được thay đổi thành công! Đang chuyển hướng về trang Đăng nhập...'
      
      // Delay 3 seconds then redirect to login page
      setTimeout(() => {
        router.push('/login')
      }, 3000)
    } else {
      errorMessage.value = 'Có lỗi xảy ra trong quá trình đổi mật khẩu.'
    }
  } catch (error) {
    if (error.response && error.response.data && error.response.data.message) {
      errorMessage.value = error.response.data.message
    } else {
      errorMessage.value = 'Không thể kết nối đến máy chủ. Vui lòng thử lại.'
    }
  } finally {
    submitting.value = false
  }
}
</script>
