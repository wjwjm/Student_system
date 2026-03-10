const { createApp, ref, onMounted } = Vue;

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

    const authHeader = () => ({ Authorization: `Bearer ${token.value}` });

    const login = async () => {
      const res = await fetch('/api/auth/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username.value, password: password.value })
      });
      const data = await res.json(); token.value = data.token; localStorage.setItem('token', data.token); await loadStudents(); await loadStats();
    };

    const loadStudents = async () => {
      const res = await fetch(`/api/students?page=${page.value}&size=${size.value}&keyword=${keyword.value}`, { headers: authHeader() });
      const data = await res.json(); students.value = data.content; total.value = data.totalElements;
    };

    const loadStats = async () => {
      const res = await fetch('/api/students/stats/employment', { headers: authHeader() });
      const data = await res.json();
      const chart = echarts.init(document.getElementById('chart'));
      chart.setOption({ xAxis: { type: 'category', data: data.map(v => `${v.college}-${v.major}-${v.year}`) }, yAxis: { type: 'value' }, series: [{ data: data.map(v => v.employmentRate), type: 'bar' }] });
    };

    onMounted(async () => { if (token.value) { await loadStudents(); await loadStats(); } });
    return { username, password, students, total, page, size, keyword, login, loadStudents };
  },
  template: `
<div>
  <h2>学生信息管理系统（离线版）</h2>
  <div class="toolbar">
    <input v-model="username" placeholder="用户名"/>
    <input v-model="password" placeholder="密码" type="password"/>
    <button @click="login">登录</button>
    <input v-model="keyword" placeholder="姓名/学号"/>
    <button @click="loadStudents">查询</button>
  </div>
  <table border="1" cellpadding="6" cellspacing="0" width="100%">
    <thead><tr><th>学号</th><th>姓名</th><th>学院</th><th>专业</th><th>入学年</th><th>就业</th></tr></thead>
    <tbody><tr v-for="s in students" :key="s.id"><td>{{s.studentNo}}</td><td>{{s.name}}</td><td>{{s.collegeName}}</td><td>{{s.majorName}}</td><td>{{s.enrollmentYear}}</td><td>{{s.employed ? '是' : '否'}}</td></tr></tbody>
  </table>
  <div id="chart"></div>
</div>`
}).mount('#app');
