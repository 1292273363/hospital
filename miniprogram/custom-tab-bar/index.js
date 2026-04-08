Component({
  data: {
    selected: 0,
    list: []
  },

  lifetimes: {
    attached() {
      this.refresh();
    }
  },

  pageLifetimes: {
    show() {
      this.refresh();
    }
  },

  methods: {
    refresh() {
      const role = wx.getStorageSync('userRole') || 'patient';
      const pages = getCurrentPages();
      const route = pages.length ? pages[pages.length - 1].route : '';
      let list = [];
      let selected = 0;
      if (role === 'doctor') {
        list = [
          { pagePath: '/pages/doctor/index', text: '首页', icon: '🏠' },
          { pagePath: '/pages/doctor/center', text: '中心', icon: '👤' }
        ];
        if (route === 'pages/doctor/index') {
          selected = 0;
        } else if (route === 'pages/doctor/center') {
          selected = 1;
        }
      } else {
        list = [
          { pagePath: '/pages/index/index', text: '首页', icon: '🏠' },
          { pagePath: '/pages/center/index', text: '中心', icon: '👤' }
        ];
        if (route === 'pages/index/index') {
          selected = 0;
        } else if (route === 'pages/center/index') {
          selected = 1;
        }
      }
      this.setData({ list, selected });
    },

    switchTab(e) {
      const url = e.currentTarget.dataset.path;
      if (!url) return;
      wx.switchTab({ url });
    }
  }
});
