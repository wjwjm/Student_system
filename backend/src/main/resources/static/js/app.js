const { createApp, ref, reactive, computed, onMounted } = Vue;

createApp({
  setup() {
    const token = ref(localStorage.getItem('token') || '');
    const username = ref('admin');
    const password = ref('admin123');

    const students = ref([]);
    const page = ref(0);
    const size = ref(10);
    const total = ref(0);
    const keyword = ref('');

    const loadingCount = ref(0);
    const isLoading = computed(() => loadingCount.value > 0);
    const errorMessage = ref('');

    const form = reactive({
      id: null,
      studentNo: '',
      name: '',
      enrollmentYear: new Date().getFullYear(),
      collegeId: 1,
      majorId: 1,
      employed: false,
    });

    const authHeader = () => ({ Authorization: `Bearer ${token.value}` });

    const clearError = () => {
      errorMessage.value = '';
    };

    const setError = (message) => {
      errorMessage.value = message || '操作失败，请稍后重试';
      if (window.ElementPlus?.ElMessage) {
        window.ElementPlus.ElMessage.error(errorMessage.value);
      }
    };

    const performRequest = async (requestFn, defaultErrorMessage = '请求失败') => {
      loadingCount.value += 1;
      clearError();
      try {
        const response = await requestFn();
        if (!response.ok) {
          let message = defaultErrorMessage;
          try {
            const payload = await response.json();
            message = payload.message || payload.error || defaultErrorMessage;
          } catch (_) {
            const text = await response.text();
            if (text) {
              message = text;
            }
          }
          throw new Error(message);
        }
        return response;
      } catch (error) {
        setError(error.message || defaultErrorMessage);
        throw error;
      } finally {
        loadingCount.value -= 1;
      }
    };

    const login = async () => {
      try {
        const response = await performRequest(() => fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: username.value, password: password.value }),
        }), '登录失败');

        const data = await response.json();
        token.value = data.token;
        localStorage.setItem('token', data.token);
        if (window.ElementPlus?.ElMessage) {
          window.ElementPlus.ElMessage.success('登录成功');
        }
        await loadStudents();
        await loadStats();
      } catch (_) {
        // error already handled in performRequest
      }
    };

    const loadStudents = async () => {
      if (!token.value) return;
      try {
        const response = await performRequest(() => fetch(`/api/students?page=${page.value}&size=${size.value}&keyword=${encodeURIComponent(keyword.value || '')}`, {
          headers: authHeader(),
        }), '加载学生列表失败');

        const data = await response.json();
        students.value = data.content || [];
        total.value = data.totalElements || 0;
      } catch (_) {
        students.value = [];
      }
    };

    const loadStats = async () => {
      if (!token.value) return;
      try {
        const response = await performRequest(() => fetch('/api/students/stats/employment', {
          headers: authHeader(),
        }), '加载统计数据失败');

        const data = await response.json();
        const chartDom = document.getElementById('chart');
        if (!chartDom) return;
        const chart = echarts.init(chartDom);
        chart.setOption({
          tooltip: {},
          xAxis: { type: 'category', data: data.map((v) => `${v.college}-${v.major}-${v.year}`) },
          yAxis: { type: 'value' },
          series: [{ data: data.map((v) => Number(v.employmentRate || 0).toFixed(2)), type: 'bar' }],
        });
      } catch (_) {
        // error already handled
      }
    };

    const resetForm = () => {
      form.id = null;
      form.studentNo = '';
      form.name = '';
      form.enrollmentYear = new Date().getFullYear();
      form.collegeId = 1;
      form.majorId = 1;
      form.employed = false;
    };

    const fillForm = (student) => {
      form.id = student.id;
      form.studentNo = student.studentNo;
      form.name = student.name;
      form.enrollmentYear = student.enrollmentYear;
      form.collegeId = student.collegeId;
      form.majorId = student.majorId;
      form.employed = Boolean(student.employed);
    };

    const submitStudent = async () => {
      if (!token.value) {
        setError('请先登录');
        return;
      }

      const payload = {
        studentNo: form.studentNo,
        name: form.name,
        enrollmentYear: Number(form.enrollmentYear),
        collegeId: Number(form.collegeId),
        majorId: Number(form.majorId),
        employed: Boolean(form.employed),
      };

      const isEdit = Boolean(form.id);
      const url = isEdit ? `/api/students/${form.id}` : '/api/students';
      const method = isEdit ? 'PUT' : 'POST';

      try {
        await performRequest(() => fetch(url, {
          method,
          headers: { ...authHeader(), 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        }), isEdit ? '更新学生失败' : '新增学生失败');

        if (window.ElementPlus?.ElMessage) {
          window.ElementPlus.ElMessage.success(isEdit ? '更新成功' : '新增成功');
        }
        resetForm();
        await loadStudents();
        await loadStats();
      } catch (_) {
        // error already handled
      }
    };

    const removeStudent = async (id) => {
      if (!token.value) {
        setError('请先登录');
        return;
      }

      if (!window.confirm('确定删除该学生吗？')) return;

      try {
        await performRequest(() => fetch(`/api/students/${id}`, {
          method: 'DELETE',
          headers: authHeader(),
        }), '删除学生失败');

        if (window.ElementPlus?.ElMessage) {
          window.ElementPlus.ElMessage.success('删除成功');
        }
        await loadStudents();
        await loadStats();
      } catch (_) {
        // error already handled
      }
    };

    const importStudents = async (event) => {
      const file = event.target.files?.[0];
      if (!file) return;
      if (!token.value) {
        setError('请先登录');
        return;
      }

      const formData = new FormData();
      formData.append('file', file);

      try {
        await performRequest(() => fetch('/api/students/import', {
          method: 'POST',
          headers: authHeader(),
          body: formData,
        }), '导入失败');

        if (window.ElementPlus?.ElMessage) {
          window.ElementPlus.ElMessage.success('导入成功');
        }
        event.target.value = '';
        await loadStudents();
        await loadStats();
      } catch (_) {
        event.target.value = '';
      }
    };

    const downloadBinary = async (url, fallbackName) => {
      if (!token.value) {
        setError('请先登录');
        return;
      }

      try {
        const response = await performRequest(() => fetch(url, { headers: authHeader() }), '下载失败');
        const blob = await response.blob();
        const contentDisposition = response.headers.get('Content-Disposition') || '';
        const filenameMatch = contentDisposition.match(/filename=([^;]+)/i);
        const filename = filenameMatch ? filenameMatch[1].replace(/"/g, '') : fallbackName;

        const objectUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = objectUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(objectUrl);
      } catch (_) {
        // error already handled
      }
    };

    const exportCsv = () => downloadBinary('/api/students/export?type=csv', 'students.csv');
    const exportXlsx = () => downloadBinary('/api/students/export?type=xlsx', 'students.xlsx');
    const downloadTemplateCsv = () => downloadBinary('/api/students/template?type=csv', 'student-template.csv');
    const downloadTemplateXlsx = () => downloadBinary('/api/students/template?type=xlsx', 'student-template.xlsx');

    onMounted(async () => {
      if (token.value) {
        await loadStudents();
        await loadStats();
      }
    });

    return {
      username,
      password,
      students,
      total,
      page,
      size,
      keyword,
      login,
      loadStudents,
      loadStats,
      form,
      submitStudent,
      resetForm,
      fillForm,
      removeStudent,
      importStudents,
      exportCsv,
      exportXlsx,
      downloadTemplateCsv,
      downloadTemplateXlsx,
      isLoading,
      errorMessage,
    };
  },
  template: `
<div>
  <h2>学生信息管理系统（离线版）</h2>

  <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
  <div v-if="isLoading" class="loading-banner">加载中，请稍候...</div>

  <div class="toolbar">
    <input v-model="username" placeholder="用户名"/>
    <input v-model="password" placeholder="密码" type="password"/>
    <button @click="login">登录</button>
  </div>

  <div class="toolbar">
    <input v-model="keyword" placeholder="姓名/学号"/>
    <button @click="loadStudents">查询</button>
    <button @click="loadStats">刷新统计</button>
  </div>

  <div class="toolbar">
    <label class="file-upload">导入 CSV/XLSX <input type="file" @change="importStudents" accept=".csv,.xlsx" /></label>
    <button @click="exportCsv">导出 CSV</button>
    <button @click="exportXlsx">导出 XLSX</button>
    <button @click="downloadTemplateCsv">模板 CSV</button>
    <button @click="downloadTemplateXlsx">模板 XLSX</button>
  </div>

  <div class="editor">
    <input v-model="form.studentNo" placeholder="学号"/>
    <input v-model="form.name" placeholder="姓名"/>
    <input v-model.number="form.enrollmentYear" type="number" placeholder="入学年"/>
    <input v-model.number="form.collegeId" type="number" placeholder="学院ID"/>
    <input v-model.number="form.majorId" type="number" placeholder="专业ID"/>
    <label><input v-model="form.employed" type="checkbox"/> 已就业</label>
    <button @click="submitStudent">{{ form.id ? '更新学生' : '新增学生' }}</button>
    <button @click="resetForm">重置</button>
  </div>

  <table border="1" cellpadding="6" cellspacing="0" width="100%">
    <thead>
      <tr><th>学号</th><th>姓名</th><th>学院</th><th>专业</th><th>入学年</th><th>就业</th><th>操作</th></tr>
    </thead>
    <tbody>
      <tr v-for="s in students" :key="s.id">
        <td>{{s.studentNo}}</td>
        <td>{{s.name}}</td>
        <td>{{s.collegeName}}</td>
        <td>{{s.majorName}}</td>
        <td>{{s.enrollmentYear}}</td>
        <td>{{s.employed ? '是' : '否'}}</td>
        <td>
          <button @click="fillForm(s)">编辑</button>
          <button @click="removeStudent(s.id)">删除</button>
        </td>
      </tr>
    </tbody>
  </table>
  <div class="page-info">共 {{ total }} 条</div>

  <div id="chart"></div>
</div>`,
}).mount('#app');
