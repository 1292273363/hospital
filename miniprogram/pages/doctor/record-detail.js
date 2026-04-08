Page({
  data: {
    patientId: null,
    patientName: '',
    record: null
  },

  onLoad(options) {
    const pid = Number(options.pid || 0);
    const rid = Number(options.rid || 0);

    const patients = [
      { id: 1, name: '张三' },
      { id: 2, name: '李四' },
      { id: 3, name: '王五' },
      { id: 4, name: '赵六' },
      { id: 5, name: '孙七' }
    ];
    const patient = patients.find((p) => p.id === pid) || patients[0] || { id: pid, name: '' };
    const baseName = patient.name || '该患者';

    const records = [
      {
        id: 1,
        name: `${baseName} 初诊面诊记录.pdf`,
        date: '2024-01-10',
        type: 'file',
        remark: '包含主诉、既往史、体检所见以及初步治疗方案等信息。'
      },
      {
        id: 2,
        name: `${baseName} 术前检查报告.jpg`,
        date: '2024-01-12',
        type: 'image',
        remark: '血常规、生化、影像学检查截图，用于评估手术风险。'
      }
    ];

    const record = records.find((r) => r.id === rid) || records[0] || null;

    this.setData({
      patientId: patient.id,
      patientName: patient.name,
      record
    });
  }
});

