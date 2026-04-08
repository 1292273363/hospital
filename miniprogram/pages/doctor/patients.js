Page({
  data: {
    searchKeyword: '',
    selectedFilter: 'all',
    filterTabs: [
      { key: 'all', label: '全部患者' },
      { key: 'pending', label: '待接诊' },
      { key: 'treating', label: '诊疗中' },
      { key: 'done', label: '已完成' }
    ],
    allPatients: [
      {
        id: 1,
        name: '张三',
        avatarText: '张',
        gender: '女',
        age: 32,
        phone: '138****5678',
        status: '待接诊',
        statusKey: 'pending',
        statusClass: 'st-pending',
        date: '2024-01-15',
        project: '面部整容手术'
      },
      {
        id: 2,
        name: '李四',
        avatarText: '李',
        gender: '男',
        age: 28,
        phone: '135****1234',
        status: '诊疗中',
        statusKey: 'treating',
        statusClass: 'st-treating',
        date: '2024-01-12',
        project: '隆鼻手术'
      },
      {
        id: 3,
        name: '王五',
        avatarText: '王',
        gender: '女',
        age: 25,
        phone: '139****8765',
        status: '已完成',
        statusKey: 'done',
        statusClass: 'st-done',
        date: '2024-01-08',
        project: '双眼皮手术'
      },
      {
        id: 4,
        name: '赵六',
        avatarText: '赵',
        gender: '女',
        age: 45,
        phone: '136****4321',
        status: '诊疗中',
        statusKey: 'treating',
        statusClass: 'st-treating',
        date: '2024-01-05',
        project: '除皱手术'
      },
      {
        id: 5,
        name: '孙七',
        avatarText: '孙',
        gender: '男',
        age: 30,
        phone: '137****9876',
        status: '待接诊',
        statusKey: 'pending',
        statusClass: 'st-pending',
        date: '2024-01-03',
        project: '下颌骨整形'
      }
    ],
    displayList: []
  },

  onLoad() {
    this.applyFilter();
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value || '' });
    this.applyFilter();
  },

  onFilterTap(e) {
    const key = e.currentTarget.dataset.key;
    if (!key || key === this.data.selectedFilter) return;
    this.setData({ selectedFilter: key });
    this.applyFilter();
  },

  applyFilter() {
    const { allPatients, selectedFilter, searchKeyword } = this.data;
    let list = allPatients.slice();
    if (selectedFilter !== 'all') {
      list = list.filter((p) => p.statusKey === selectedFilter);
    }
    const kw = (searchKeyword || '').trim();
    if (kw) {
      list = list.filter((p) => p.name.includes(kw) || p.phone.includes(kw));
    }
    this.setData({ displayList: list });
  },

  onViewRecord(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/doctor/patient-record?id=${id}`
    });
  },

  onViewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/doctor/patient-detail?id=${id}`
    });
  }
});
